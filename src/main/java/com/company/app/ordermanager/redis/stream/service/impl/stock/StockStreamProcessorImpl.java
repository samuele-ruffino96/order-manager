package com.company.app.ordermanager.redis.stream.service.impl.stock;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.exception.orderitem.OrderItemsNotFoundException;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.stock.StockUpdateException;
import com.company.app.ordermanager.redis.stream.common.StreamFields;
import com.company.app.ordermanager.redis.stream.common.StreamNames;
import com.company.app.ordermanager.redis.stream.dto.StockUpdateMessage;
import com.company.app.ordermanager.redis.stream.service.api.stock.StockStreamProcessor;
import com.company.app.ordermanager.repository.api.orderitem.OrderItemRepository;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockStreamProcessorImpl implements StockStreamProcessor {
    private static final String GROUP_NAME = "stock-processor-group";
    private static final String CONSUMER_NAME = "consumer-1";
    private static final String PRODUCT_LOCK_KEY_PREFIX = "product:lock:";

    private static final int STREAM_BATCH_SIZE = 1;
    private static final Duration STREAM_WAIT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(5);

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

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

    @Scheduled(fixedDelay = 2000)
    public void processStockUpdateMessages() {
        // Read new messages from the stream using the new API
        Map<StreamMessageId, Map<String, String>> entries = stream.readGroup(
                GROUP_NAME,
                CONSUMER_NAME,
                StreamReadGroupArgs.greaterThan(StreamMessageId.ALL)
                        .count(STREAM_BATCH_SIZE)
                        .timeout(STREAM_WAIT_TIMEOUT)
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
                log.error("Failed to parse stock update message: {}. Error: {}", message, e.getMessage());

                // Acknowledge the message since it is not a valid message
                stream.ack(GROUP_NAME, messageId);

                // Trim the stream to ensure it doesn't grow indefinitely.
                stream.trim(StreamTrimArgs.maxLen(1000).noLimit());
            } catch (ProductNotFoundException e) {
                log.error("Failed to find product within stock update message: {}. Error: {}", message, e.getMessage());

                // Acknowledge the message since it is not a valid message
                stream.ack(GROUP_NAME, messageId);

                // Trim the stream to ensure it doesn't grow indefinitely.
                stream.trim(StreamTrimArgs.maxLen(1000).noLimit());
            } catch (OrderItemsNotFoundException e) {
                log.error("Failed to find order item within stock update message: {}. Error: {}", message, e.getMessage());

                // Acknowledge the message since it is not a valid message
                stream.ack(GROUP_NAME, messageId);

                // Trim the stream to ensure it doesn't grow indefinitely.
                stream.trim(StreamTrimArgs.maxLen(1000).noLimit());
            } catch (StockUpdateException e) {
                log.warn("Failed to acquire lock for product within stock update message: {}. Error: {}", message, e.getMessage());

                // Don't send ack so message could be reprocessed later
            } catch (Exception e) {
                log.error("Failed to process stock update message: {}. Error: {}", message, e.getMessage());

                // Unexpected exception occur, acknowledge the message to avoid reprocessing
                stream.ack(GROUP_NAME, messageId);

                // Trim the stream to ensure it doesn't grow indefinitely.
                stream.trim(StreamTrimArgs.maxLen(1000).noLimit());
            }
        }
    }

    /**
     * Processes a stock update message by delegating to the appropriate handler
     * based on the type of update specified in the message.
     *
     * @param message a {@link StockUpdateMessage} object containing details about
     *                the stock update.
     * @throws StockUpdateException     If the lock cannot be acquired within the timeout period,
     *                                  indicating it is not possible to proceed with the requested operation.
     * @throws ProductNotFoundException If the product with the given {@code productId}
     *                                  is not found in the database.
     * @throws OrderItemsNotFoundException If the order item specified within message is not found.
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
     * @throws StockUpdateException     If the lock cannot be acquired within the timeout period,
     *                                  indicating it is not possible to proceed with the requested operation.
     * @throws ProductNotFoundException If the product with the given {@code productId}
     *                                  is not found in the database.
     * @throws OrderItemsNotFoundException If the order item specified within message is not found.
     */
    private void handleStockReservation(StockUpdateMessage message) {
        // Get product lock
        RLock lock = redissonClient.getLock(getProductLockKey(message.getProductId()));

        try {
            tryLock(message.getProductId(), lock);

            int available = getCurrentStock(message.getProductId());

            if (available < message.getQuantity()) {
                log.debug("Insufficient stock for product: {}. Available: {}, Requested: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity());

                // Update order item status to cancelled
                updateOrderItem(
                        message.getOrderItemId(),
                        message.getExpectedOrderItemVersion(),
                        message.getOrderId(),
                        message.getProductId(),
                        OrderItemStatus.CANCELLED,
                        "INSUFFICIENT_STOCK"
                );
            } else {
                log.debug("Stock available for product: {}. Available: {}, Requested: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity());

                // Update order item status to confirmed
                updateOrderItem(
                        message.getOrderItemId(),
                        message.getExpectedOrderItemVersion(),
                        message.getOrderId(),
                        message.getProductId(),
                        OrderItemStatus.CONFIRMED,
                        null
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
     * @throws StockUpdateException     If the lock cannot be acquired within the timeout period,
     *                                  indicating it is not possible to proceed with the requested operation.
     * @throws ProductNotFoundException If the product with the given {@code productId}
     *                                  is not found in the database.
     * @throws NumberFormatException    If the stock level retrieved from the cache cannot
     *                                  be parsed to an integer.
     * @throws OrderItemsNotFoundException If the order item specified within message is not found.
     */
    private void handleStockCancellation(StockUpdateMessage message) {
        // Get product lock
        RLock lock = redissonClient.getLock(getProductLockKey(message.getProductId()));

        try {
            tryLock(message.getProductId(), lock);

            // Update order item status to cancelled
            updateOrderItem(
                    message.getOrderItemId(),
                    message.getExpectedOrderItemVersion(),
                    message.getOrderId(),
                    message.getProductId(),
                    OrderItemStatus.CANCELLED,
                    null
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
     *
     * @param productId The {@link UUID} representing the unique identifier of the product.
     *                  Must not be {@code null}.
     * @return The current stock level of the product as an integer.
     * @throws ProductNotFoundException If the product with the given {@code productId}
     *                                  is not found in the database.
     */
    private int getCurrentStock(UUID productId) {
        // TODO: use redis cache
        // If stock not in cache, fetch from database

        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId))
                .getStockLevel();
    }

    /**
     * Updates the stock level of a specific product.
     *
     * @param productId     the unique identifier of the product whose stock level will be updated.
     * @param newStockLevel the new stock level for the product. Must be a non-negative integer.
     */
    private void updateStockLevel(UUID productId, int newStockLevel) {
        //TODO: cache to redis and Send update stock level request to a queue

        productRepository.updateStockLevel(productId, newStockLevel);
    }

    /**
     * Updates the status of a specific order item within an order. The method ensures that the update is only performed if
     * the current version of the order item matches the expected version, preventing overwriting a newer status. If an
     * error message is provided, it is also saved along with the status update.
     *
     * @param orderItemId             The {@link UUID} of the order item to be updated. This uniquely identifies the specific item within an order.
     * @param expectedOrderItemStatus The expected version of the order item. If the current version does not match this value,
     *                                the update is not performed to avoid overwriting newer changes.
     * @param orderId                 The {@link UUID} of the order containing the order item. This identifies the parent order of the item.
     * @param productId               The {@link UUID} of the product associated with the order item. This identifies the product being updated.
     * @param status                  The {@link OrderItemStatus} representing the new status to be set for the order item.
     * @param error                   An optional error message to associate with the order item. If {@literal null}, no error message is set.
     * @throws OrderItemsNotFoundException If the order item with the specified {@code orderItemId} is not found in the repository.
     */
    private void updateOrderItem(UUID orderItemId, long expectedOrderItemStatus, UUID orderId, UUID productId, OrderItemStatus status, String error) {
        long version = orderItemRepository.findVersionById(orderItemId).orElseThrow(() -> new OrderItemsNotFoundException(orderId, Set.of(orderItemId)));

        if (!Objects.equals(expectedOrderItemStatus, version)) {
            // If we don't return, we could overwrite a newer status
            return;
        }

        if (error != null) {
            orderItemRepository.updateStatusAndError(orderItemId, productId, orderId, status, error);
        } else {
            orderItemRepository.updateStatus(orderItemId, productId, orderId, status);
        }
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

    private String getProductLockKey(UUID productId) {
        return PRODUCT_LOCK_KEY_PREFIX + productId.toString();
    }
}
