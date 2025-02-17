package com.company.app.ordermanager.redis.stream.service.impl.orderitem;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.redis.stream.common.StreamFields;
import com.company.app.ordermanager.redis.stream.common.StreamNames;
import com.company.app.ordermanager.redis.stream.dto.OrderItemStatusUpdateMessage;
import com.company.app.ordermanager.redis.stream.service.api.orderitem.OrderItemStreamProcessor;
import com.company.app.ordermanager.redis.stream.service.api.orderitem.OrderItemStreamService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemStreamServiceImpl implements OrderItemStreamService {
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    public void sendOrderItemStatusUpdateMessage(UUID orderId, UUID orderItemId, UUID productId, OrderItemStatus newStatus) {
        Assert.notNull(orderId, "Order ID must not be null");
        Assert.notNull(orderItemId, "Order item ID must not be null");
        Assert.notNull(newStatus, "New status must not be null");

        sendOrderItemStatusUpdateMessage(orderId, orderItemId, productId, newStatus, null);
    }

    @Override
    public void sendOrderItemStatusUpdateMessage(UUID orderId, UUID orderItemId, UUID productId, OrderItemStatus newStatus, String error) {
        Assert.notNull(orderId, "Order ID must not be null");
        Assert.notNull(orderItemId, "Order item ID must not be null");
        Assert.notNull(orderItemId, "Product ID must not be null");
        Assert.notNull(newStatus, "New status must not be null");

        OrderItemStatusUpdateMessage message = OrderItemStatusUpdateMessage.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .productId(productId)
                .newStatus(newStatus)
                .error(error)
                .build();

        try {
            publishOrderItemStatusUpdateMessage(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish order item status update message. Error: {}. Message: {}", e.getMessage(), message);
        }
    }

    /**
     * Publishes an order item status update message to a Redis stream for processing by {@link OrderItemStreamProcessor}.
     *
     * <p>This method serializes the provided {@link OrderItemStatusUpdateMessage} object into a JSON string
     * and publishes it to the Redis stream identified by {@link StreamNames#ORDER_ITEM_STATUS_UPDATE_QUEUE}.
     *
     * @param message The {@link OrderItemStatusUpdateMessage} object containing details about the order item status update to be published.
     * @return The {@link StreamMessageId} representing the unique identifier assigned to the published message
     * by the Redis stream.
     * @throws JsonProcessingException If the input object cannot be serialized into a JSON string.
     */
    private StreamMessageId publishOrderItemStatusUpdateMessage(OrderItemStatusUpdateMessage message) throws JsonProcessingException {
        RStream<String, String> stream = redissonClient.getStream(StreamNames.ORDER_ITEM_STATUS_UPDATE_QUEUE.getKey());

        String messageJson = objectMapper.writeValueAsString(message);

        StreamMessageId id = stream.add(StreamAddArgs.entry(StreamFields.MESSAGE.getField(), messageJson));

        log.debug("Published order item status update message with ID {}. Message: {}", id, messageJson);

        return id;
    }
}
