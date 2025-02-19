package com.company.app.ordermanager.messaging.service.impl.stock;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.stock.StockLockException;
import com.company.app.ordermanager.messaging.redis.StreamFields;
import com.company.app.ordermanager.messaging.common.MessageChannels;
import com.company.app.ordermanager.messaging.dto.StockUpdateMessage;
import com.company.app.ordermanager.messaging.service.api.stock.StockMessageConsumerService;
import com.company.app.ordermanager.service.api.orderitem.OrderItemService;
import com.company.app.ordermanager.service.api.product.ProductService;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMessageConsumerServiceImpl implements StockMessageConsumerService {
    private static final String GROUP_NAME = "stock-processor-group";
    private static final String CONSUMER_NAME = "consumer" + UUID.randomUUID();

    private static final String PRODUCT_LOCK_KEY_PREFIX = "product:lock:";
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);

    private static final int STREAM_BATCH_SIZE = 1;
    private static final Duration STREAM_WAIT_TIMEOUT = Duration.ofSeconds(10);

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final OrderItemService orderItemService;
    private final ProductService productService;

    private RStream<String, String> stream;

    @PostConstruct
    private void init() {
        initializeStream();
    }

    /**
     * Periodically processes stock update messages from a message stream.
     * This method reads stock update messages from a stream, parses them,
     * processes the corresponding stock updates, and acknowledges the messages
     * to mark them as processed. Messages that cause exceptions during processing
     * are either skipped for reprocessing (in some cases) or acknowledged
     * to prevent indefinite retries. It also performs stream trimming to limit
     * its size and maintain performance.
     * <p>
     * This method is automatically invoked with a fixed delay of 2000ms.
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processStockUpdateMessages() {
        // Read new messages from the stream using the new API
        Map<StreamMessageId, Map<String, String>> entries = stream.readGroup(
                GROUP_NAME,
                CONSUMER_NAME,
                StreamReadGroupArgs.greaterThan(StreamMessageId.ALL)
                        .count(STREAM_BATCH_SIZE)
                        .timeout(STREAM_WAIT_TIMEOUT)
        );

        log.debug("Read {} messages from stream", entries.size());

        for (Map.Entry<StreamMessageId, Map<String, String>> entry : entries.entrySet()) {
            StreamMessageId messageId = entry.getKey();
            Map<String, String> message = entry.getValue();

            try {
                log.debug("Processing stock update message: {}", message);

                StockUpdateMessage stockUpdateMessage = parseMessage(message);

                processStockUpdateMessage(stockUpdateMessage);

                // Acknowledge the message to mark it as processed
                stream.ack(GROUP_NAME, messageId);

                log.debug("Processed stock update message with id: {}", messageId.toString());

                // Trim the stream to ensure it doesn't grow indefinitely.
                stream.trim(StreamTrimArgs.maxLen(1000).noLimit());
            } catch (StockLockException e) {
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
     * Processes a stock update based on the type of the received message.
     *
     * @param message the {@link StockUpdateMessage} containing details about the stock update.
     * @throws IllegalArgumentException if the {@code message} contains invalid or inconsistent data.
     * @throws ProductNotFoundException if no product is found with the product ID within the stock update message
     * @throws StockLockException if the method is interrupted while acquiring the product lock
     */
    @Override
    public void processStockUpdateMessage(StockUpdateMessage message) {
        switch (message.getUpdateType()) {
            case RESERVE -> handleStockReservation(message);
            case CANCEL -> handleStockCancellation(message);
        }
    }

    /**
     * Handles the reservation of stock for a given product. Determines if the requested quantity
     * can be reserved and updates the stock levels and order item status accordingly.
     *
     * @param message the {@link StockUpdateMessage} object containing details for stock reservation such as
     *                the product ID, order item ID, quantity to be reserver, and expected order item version.
     * @throws IllegalArgumentException if the {@code message} contains invalid or inconsistent data.
     * @throws ProductNotFoundException if no product is found with the product ID within the stock update message
     * @throws StockLockException       if the method is interrupted while acquiring the product lock
     */
    private void handleStockReservation(StockUpdateMessage message) {
        // Get product lock
        RLock lock = redissonClient.getLock(getProductLockKey(message.getProductId()));

        try {
            tryLock(message.getProductId(), lock);

            int available = productService.getProductStockLevel(message.getProductId());

            if (available < message.getQuantity()) {
                log.debug("Insufficient stock for product: {}. Available: {}, Requested: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity());

                // Update order item status to cancelled
                orderItemService.updateOrderItemStatusAndReason(
                        message.getOrderItemId(),
                        OrderItemStatus.CANCELLED,
                        message.getExpectedOrderItemVersion(),
                        OrderItemStatusReason.INSUFFICIENT_STOCK
                );
            } else {
                log.debug("Stock available for product: {}. Available: {}, Requested: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity());

                // Update order item status to confirmed
                orderItemService.updateOrderItemStatus(
                        message.getOrderItemId(),
                        OrderItemStatus.CONFIRMED,
                        message.getExpectedOrderItemVersion()
                );

                // Calc new stock level
                int updatedStockLevel = available - message.getQuantity();

                // Update stock level
                productService.updateProductStockLevel(message.getProductId(), updatedStockLevel);

                log.debug("Updated stock level for product: {}. Available: {}, Requested: {}, New: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity(),
                        updatedStockLevel);
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while acquiring lock for product: {}. Error: {}", message.getProductId(), e.getMessage());

            handleStockLockAcquisitionFailure(message.getProductId());
        } finally {
            log.debug("Releasing lock for product: {}", message.getProductId());

            releaseLock(lock);
        }

    }

    /**
     * Handles the stock cancellation process by updating stock levels and order item status.
     *
     * @param message the {@link StockUpdateMessage} object containing details for stock cancellation such as
     *                the product ID, order item ID, quantity to be cancelled, and expected order item version.
     * @throws IllegalArgumentException if the {@code message} contains invalid or inconsistent data.
     * @throws ProductNotFoundException if no product is found with the product ID within the stock update message
     * @throws StockLockException       if the method is interrupted while acquiring the product lock
     */
    private void handleStockCancellation(StockUpdateMessage message) {
        // Get product lock
        RLock lock = redissonClient.getLock(getProductLockKey(message.getProductId()));

        try {
            tryLock(message.getProductId(), lock);

            // Update order item status to cancelled
            orderItemService.updateOrderItemStatus(
                    message.getOrderItemId(),
                    OrderItemStatus.CANCELLED,
                    message.getExpectedOrderItemVersion()
            );

            int available = productService.getProductStockLevel(message.getProductId());

            // Calc new stock level
            int updatedStockLevel = available + message.getQuantity();

            // Update stock level
            productService.updateProductStockLevel(message.getProductId(), updatedStockLevel);

            log.debug("Updated stock level for product: {}. Available: {}, Requested: {}, New: {}",
                    message.getProductId(),
                    available,
                    message.getQuantity(),
                    updatedStockLevel);
        } catch (InterruptedException e) {
            log.warn("Interrupted while acquiring lock for product: {}. Error: {}", message.getProductId(), e.getMessage());

            handleStockLockAcquisitionFailure(message.getProductId());
        } finally {
            log.debug("Releasing lock for product: {}", message.getProductId());

            releaseLock(lock);
        }
    }

    /**
     * Attempts to acquire a lock for a specified product within a defined timeout period.
     * Throws an exception if the lock cannot be acquired within the timeout.
     *
     * @param productId the unique identifier of the product for which the lock is being attempted
     * @param lock the lock object representing the lock to be acquired
     * @throws InterruptedException if the current thread is interrupted while waiting to acquire the lock
     */
    private void tryLock(UUID productId, RLock lock) throws InterruptedException {
        if (!lock.tryLock(LOCK_TIMEOUT.getSeconds(), TimeUnit.SECONDS)) {
            throw new StockLockException("Could not acquire lock for product: " + productId.toString());
        }
    }

    /**
     * Releases the provided lock if it is held by the current thread.
     *
     * @param lock the RLock instance to be released
     */
    private void releaseLock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * Handles the failure to acquire a stock lock for a given product and throw a custom exception.
     *
     * @param productId the unique identifier of the product for which the lock acquisition failed
     * @throws StockLockException
     */
    private void handleStockLockAcquisitionFailure(UUID productId) {
        Thread.currentThread().interrupt();
        throw new StockLockException("Failed to acquire lock for product: " + productId.toString());
    }

    /**
     * Initializes a Redis stream for message consumption using Redisson.
     * This method creates a consumer group for the specified stream and ensures the stream exists.
     * If the consumer group already exists, it logs a message and does not attempt to recreate the group.
     * Any errors encountered during the initialization are logged appropriately.
     * <p>
     * The stream is retrieved using the configured Redisson client and
     * is associated with a message channel defined in the `MessageChannels` enum.
     * The consumer group is created to start consuming messages from the beginning of the stream.
     * <p>
     * Exception Handling:
     * - If the consumer group already exists (indicated by the "BUSYGROUP" Redis error),
     *   a log entry is generated stating that the group already exists.
     * - For other Redis-related errors, an error log is generated with the exception details.
     */
    private void initializeStream() {
        stream = redissonClient.getStream(MessageChannels.STOCK_UPDATE_QUEUE.getKey());
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

    /**
     * Parses a given message map to extract and deserialize a stock update message.
     *
     * @param message the map containing the message details, with keys representing
     *                message fields and values representing their contents
     * @return the deserialized StockUpdateMessage object extracted from the message map
     * @throws JsonProcessingException if there is an error during the deserialization process
     */
    private StockUpdateMessage parseMessage(Map<String, String> message) throws JsonProcessingException {
        String messageJson = message.get(StreamFields.MESSAGE.getField());
        return objectMapper.readValue(messageJson, StockUpdateMessage.class);
    }

    /**
     * Generates a lock key for the given product ID by combining a predefined prefix
     * with the string representation of the product ID.
     *
     * @param productId the UUID of the product for which the lock key is generated
     * @return the generated lock key as a string
     */
    private String getProductLockKey(UUID productId) {
        return PRODUCT_LOCK_KEY_PREFIX + productId.toString();
    }
}
