package com.company.app.ordermanager.redis.stream.service.impl.stock;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.stock.StockUpdateException;
import com.company.app.ordermanager.redis.stream.common.StreamFields;
import com.company.app.ordermanager.redis.stream.common.StreamNames;
import com.company.app.ordermanager.redis.stream.dto.StockUpdateMessage;
import com.company.app.ordermanager.redis.stream.service.api.orderitem.OrderItemStreamService;
import com.company.app.ordermanager.redis.stream.service.api.product.ProductStreamService;
import com.company.app.ordermanager.redis.stream.service.api.stock.StockStreamProcessor;
import com.company.app.ordermanager.repository.api.product.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.api.stream.StreamTrimArgs;
import org.redisson.client.RedisException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockStreamProcessorImpl implements StockStreamProcessor {
    private static final String GROUP_NAME = "stock-processor-group";
    private static final String CONSUMER_NAME = "consumer-1";
    private static final String STOCK_LOCK_KEY_PREFIX = "product:lock:";
    private static final String STOCK_VALUE_KEY_PREFIX = "stock:";

    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CACHE_EXPIRY = Duration.ofHours(1);

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final OrderItemStreamService orderItemStreamService;
    private final ProductStreamService productStreamService;

    private final ProductRepository productRepository;

    private RStream<String, String> stream;

    @PostConstruct
    private void init() {
        stream = redissonClient.getStream(StreamNames.STOCK_UPDATE_QUEUE.getKey());

        try {
            StreamCreateGroupArgs groupArgs = StreamCreateGroupArgs
                    .name(GROUP_NAME)
                    .makeStream()    // Creates the stream if it doesn't exist, removing need for our manual creation
                    .id(StreamMessageId.ALL);  // Start consuming from the beginning of the stream

            stream.createGroup(groupArgs);

            log.info("Created consumer group: {}", GROUP_NAME);
        } catch (RedisException e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("Consumer group {} already exists", GROUP_NAME);
            } else {
                log.error("Failed to initialize consumer group. Error: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    public void processStockUpdateMessages() {
        // Read new messages from the stream using the new API
        Map<StreamMessageId, Map<String, String>> entries = stream.readGroup(
                GROUP_NAME,
                CONSUMER_NAME,
                StreamReadGroupArgs.greaterThan(StreamMessageId.ALL)
                        .count(1)
                        .timeout(Duration.ofMillis(5000))
        );

        for (Map.Entry<StreamMessageId, Map<String, String>> entry : entries.entrySet()) {
            StreamMessageId messageId = entry.getKey();
            Map<String, String> message = entry.getValue();

            try {
                StockUpdateMessage stockUpdateMessage = parseMessage(message);
                processStockUpdate(stockUpdateMessage);

                /*
                 * TODO: If a consumer processes a stock update but fails to send the acknowledgment
                 *  (for example, due to a crash or network issue), the message remains unacknowledged
                 *  and will eventually be redelivered to another consumer in the group.
                 *  This can lead to the same update being applied a second time.
                 *  Wen can't have idempotent handler in this case so
                 *  we need to tracking received messages and discarding Duplicates.
                 *  Maybe we could use redis to cache last N received message
                 *
                 */

                // Acknowledge the message to mark it as processed
                stream.ack(GROUP_NAME, messageId);

                // Trim the stream to ensure it doesn't grow indefinitely.
                stream.trim(StreamTrimArgs.maxLen(1000).noLimit());
            } catch (JsonProcessingException e) {
                log.error("Failed to parse message: {}. Error: {}", message, e.getMessage());

                // Acknowledge the message since it is not a valid message
                stream.ack("processor-group", messageId);

                // Trim the stream to ensure it doesn't grow indefinitely.
                stream.trim(StreamTrimArgs.maxLen(1000).noLimit());
            } catch (StockUpdateException e) {
                log.warn("Failed to process stock update message with id: {}. Error: {}", messageId, e.getMessage());

                // Don't send ack so message could be reprocessed later
            }
        }
    }

    /**
     * Processes a stock update message by delegating to the appropriate handler
     * based on the type of update specified in the message.
     *
     * @param message a {@link StockUpdateMessage} object containing details about
     *                the stock update.
     * @throws StockUpdateException If the lock cannot be acquired within the timeout period,
     *                              indicating it is not possible to proceed with the requested operation.
     */
    private void processStockUpdate(StockUpdateMessage message) {
        switch (message.getUpdateType()) {
            case RESERVE -> handleStockReservation(message);
            case CANCEL -> handleStockCancellation(message);
        }
    }

    /**
     * Handles stock reservation for a specific product in an order by checking the available stock and
     * updating the stock levels. Based on the availability, it updates the order item status
     * to either {@link OrderItemStatus#CONFIRMED} or {@link OrderItemStatus#CANCELLED}.
     *
     * @param message a {@link StockUpdateMessage} object containing details about
     *                the stock update.
     * @throws StockUpdateException If the lock cannot be acquired within the timeout period,
     *                              indicating it is not possible to proceed with the requested operation.
     */
    private void handleStockReservation(StockUpdateMessage message) {
        // Get product lock
        RLock lock = redissonClient.getLock(getLockKey(message.getProductId()));

        try {
            tryLock(message.getProductId(), lock);

            int available = getCurrentStock(message.getProductId());

            if (available < message.getQuantity()) {
                log.debug("Insufficient stock for product: {}. Available: {}, Requested: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity());

                // Send order item cancelled request to queue
                orderItemStreamService.sendOrderItemStatusUpdateMessage(
                        message.getOrderId(),
                        message.getOrderItemId(),
                        OrderItemStatus.CANCELLED,
                        "INSUFFICIENT_STOCK"
                );
            } else {
                log.debug("Stock available for product: {}. Available: {}, Requested: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity());

                // Send order item confirmed request to queue
                orderItemStreamService.sendOrderItemStatusUpdateMessage(
                        message.getOrderId(),
                        message.getOrderItemId(),
                        OrderItemStatus.CONFIRMED
                );

                // Calc new stock level
                int updatedStockLevel = available - message.getQuantity();

                log.debug("Updated stock level for product: {}. Available: {}, Requested: {}, New: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity(),
                        updatedStockLevel);

                // Update stock level
                updateStockLevel(message.getProductId(), updatedStockLevel);
            }
        } catch (InterruptedException e) {
            handleLockAcquisitionFailure(message.getProductId(), e);
        } finally {
            releaseLock(lock);
        }

    }

    /**
     * Handles the cancellation of stock for a specific product in an order. This method updates
     * the stock levels of the product by adding back the quantity from the cancelled user request.
     *
     * @param message a {@link StockUpdateMessage} object containing details about
     *                the stock update.
     * @throws StockUpdateException If the lock cannot be acquired within the timeout period,
     *                              indicating it is not possible to proceed with the requested operation.
     */
    private void handleStockCancellation(StockUpdateMessage message) {
        // Get product lock
        RLock lock = redissonClient.getLock(getLockKey(message.getProductId()));

        try {
            tryLock(message.getProductId(), lock);

            // Send order item cancelled request to queue
            orderItemStreamService.sendOrderItemStatusUpdateMessage(
                    message.getOrderId(),
                    message.getOrderItemId(),
                    OrderItemStatus.CONFIRMED
            );

            int available = getCurrentStock(message.getProductId());

            // Calc new stock level
            int updatedStockLevel = available + message.getQuantity();

            log.debug("Updated stock level for product: {}. Available: {}, Requested: {}, New: {}",
                    message.getProductId(),
                    available,
                    message.getQuantity(),
                    updatedStockLevel);

            // Update stock level
            updateStockLevel(message.getProductId(), updatedStockLevel);
        } catch (InterruptedException e) {
            handleLockAcquisitionFailure(message.getProductId(), e);
        } finally {
            releaseLock(lock);
        }
    }

    /**
     * Attempts to acquire a distributed lock for the specified product within a defined timeout.
     * This method blocks for a specified amount of time while attempting to acquire the lock.
     * If the lock is successfully acquired, the method returns without issue. If the lock cannot
     * be acquired within the timeout, it throws a custom {@link StockUpdateException}.
     *
     * @param productId The unique identifier {@link UUID} of the product for which
     *                  the lock is being acquired.
     * @param lock      The {@link RLock} instance representing the distributed lock to be acquired.
     * @throws InterruptedException If the current thread is interrupted while waiting to acquire the lock.
     * @throws StockUpdateException If the lock cannot be acquired within the timeout period,
     *                              indicating it is not possible to proceed with the requested operation.
     */
    private void tryLock(UUID productId, RLock lock) throws InterruptedException {
        if (!lock.tryLock(LOCK_TIMEOUT.getSeconds(), TimeUnit.SECONDS)) {
            throw new StockUpdateException("Could not acquire lock for product: " + productId.toString());
        }
    }

    /**
     * Provides functionality to release a reentrant lock held by the current thread.
     * This method ensures that the lock is released only if it is held by the thread
     */
    private void releaseLock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * Handles the failure to acquire a lock for a specified product during a stock update operation.
     * This method interrupts the current thread and throws a {@link StockUpdateException} to signal
     * the failure to acquire the lock.
     *
     * @param productId the {@link UUID} of the product for which the lock acquisition failed
     * @param e         the {@link InterruptedException} that caused the lock acquisition failure
     * @throws StockUpdateException if the lock cannot be acquired for the specified product
     */
    private void handleLockAcquisitionFailure(UUID productId, InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StockUpdateException("Failed to acquire lock for product: " + productId.toString(), e);
    }

    /**
     * Retrieves the current stock level for the specified product.
     * <p>
     * This method attempts to get the stock level from a cache (Redis). If the stock
     * level is not available in the cache, it fetches the product information from
     * the database and updates the cache.
     * </p>
     *
     * @param productId The {@link UUID} representing the unique identifier of the product.
     *                  Must not be {@code null}.
     * @return The current stock level of the product as an integer.
     * @throws ProductNotFoundException If the product with the given {@code productId}
     *                                  is not found in the database.
     * @throws NumberFormatException    If the stock level retrieved from the cache cannot
     *                                  be parsed to an integer.
     */
    private int getCurrentStock(UUID productId) {
        String stockKey = getStockKey(productId);

        String currentStock = redisTemplate.opsForValue().get(stockKey);

        // If stock not in cache, fetch from database
        if (currentStock == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            currentStock = String.valueOf(product.getStockLevel());

            // Cache the stock with 1 hour expiry
            redisTemplate.opsForValue().set(stockKey, currentStock, CACHE_EXPIRY);
        }

        return Integer.parseInt(currentStock);
    }

    /**
     * Updates the stock level of a specific product. This method updates the stock level
     * both in a cache and by sending an update request to the product stock level update queue.
     *
     * @param productId     the unique identifier of the product whose stock level will be updated.
     * @param newStockLevel the new stock level for the product. Must be a non-negative integer.
     */
    private void updateStockLevel(UUID productId, int newStockLevel) {
        String stockKey = getStockKey(productId);

        // Update cache
        redisTemplate.opsForValue().set(stockKey, String.valueOf(newStockLevel));

        // Send update stock level request to queue
        productStreamService.sendProductStockLevelUpdateMessage(productId, newStockLevel);
    }

    /**
     * Utility method that constructs a unique stock key for a given product.
     * The generated key combines a predefined prefix with the string representation
     * of the provided {@link UUID} of the product.
     *
     * @param productId A {@link UUID} representing the unique identifier of the product.
     *                  It must not be {@code null}.
     * @return A {@link String} representing the unique stock key, created by appending the
     * {@literal productId} to a predefined prefix.
     */
    private String getStockKey(UUID productId) {
        return STOCK_VALUE_KEY_PREFIX + productId.toString();
    }

    /**
     * Utility method to generate a unique lock key for a specific product.
     * This key is typically used to manage stock locking operations for concurrency control.
     *
     * @param productId the {@link UUID} representing the unique identifier of the product.
     *                  This is a required parameter and cannot be {@code null}.
     * @return a {@link String} representing the lock key, which is a combination of
     * {@literal STOCK_LOCK_KEY_PREFIX} and the {@code productId} in its string form.
     */
    private String getLockKey(UUID productId) {
        return STOCK_LOCK_KEY_PREFIX + productId.toString();
    }

    /**
     * Parses a stock update message from the provided map and converts it into a {@link StockUpdateMessage} object.
     * <p>
     * This method extracts the JSON string associated with the {@literal MESSAGE} field in the input map
     * and deserializes it into an instance of {@link StockUpdateMessage}.
     * </p>
     *
     * @param message the map containing the stock update message data. It is expected to contain an entry
     *                with the key corresponding to {@link StreamFields#MESSAGE} and value as a JSON string
     *                representing a {@link StockUpdateMessage}.
     * @return a {@link StockUpdateMessage} object deserialized from the JSON string in the input map.
     * @throws JsonProcessingException if the JSON string cannot be parsed or does not match the
     *                                 {@link StockUpdateMessage} structure.
     */
    private StockUpdateMessage parseMessage(Map<String, String> message) throws JsonProcessingException {
        String messageJson = message.get(StreamFields.MESSAGE.getField());
        return objectMapper.readValue(messageJson, StockUpdateMessage.class);
    }
}
