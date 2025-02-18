package com.company.app.ordermanager.redis.stream.service.impl.stock;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
import com.company.app.ordermanager.exception.stock.StockLockException;
import com.company.app.ordermanager.redis.stream.common.StreamFields;
import com.company.app.ordermanager.redis.stream.common.StreamNames;
import com.company.app.ordermanager.redis.stream.dto.StockUpdateMessage;
import com.company.app.ordermanager.redis.stream.service.api.stock.StockStreamProcessor;
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

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockStreamProcessorImpl implements StockStreamProcessor {
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
                log.debug("Processing stock update message: {}", message);

                StockUpdateMessage stockUpdateMessage = parseMessage(message);

                processStockUpdate(stockUpdateMessage);

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

    @PostConstruct
    private void init() {
        initializeStream();
    }

    private void initializeStream() {
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

    private void processStockUpdate(StockUpdateMessage message) {
        switch (message.getUpdateType()) {
            case RESERVE -> handleStockReservation(message);
            case CANCEL -> handleStockCancellation(message);
        }
    }

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

                log.debug("Updated stock level for product: {}. Available: {}, Requested: {}, New: {}",
                        message.getProductId(),
                        available,
                        message.getQuantity(),
                        updatedStockLevel);

                // Update stock level
                productService.updateProductStockLevel(message.getProductId(), updatedStockLevel);
            }
        } catch (InterruptedException e) {
            handleStockLockAcquisitionFailure(message.getProductId());
        } finally {
            releaseLock(lock);
        }

    }

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

            log.debug("Updated stock level for product: {}. Available: {}, Requested: {}, New: {}",
                    message.getProductId(),
                    available,
                    message.getQuantity(),
                    updatedStockLevel);

            // Update stock level
            productService.updateProductStockLevel(message.getProductId(), updatedStockLevel);
        } catch (InterruptedException e) {
            handleStockLockAcquisitionFailure(message.getProductId());
        } finally {
            releaseLock(lock);
        }
    }

    private void tryLock(UUID productId, RLock lock) throws InterruptedException {
        if (!lock.tryLock(LOCK_TIMEOUT.getSeconds(), TimeUnit.SECONDS)) {
            throw new StockLockException("Could not acquire lock for product: " + productId.toString());
        }
    }

    private void releaseLock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private void handleStockLockAcquisitionFailure(UUID productId) {
        Thread.currentThread().interrupt();
        throw new StockLockException("Failed to acquire lock for product: " + productId.toString());
    }

    private StockUpdateMessage parseMessage(Map<String, String> message) throws JsonProcessingException {
        String messageJson = message.get(StreamFields.MESSAGE.getField());
        return objectMapper.readValue(messageJson, StockUpdateMessage.class);
    }

    private String getProductLockKey(UUID productId) {
        return PRODUCT_LOCK_KEY_PREFIX + productId.toString();
    }
}
