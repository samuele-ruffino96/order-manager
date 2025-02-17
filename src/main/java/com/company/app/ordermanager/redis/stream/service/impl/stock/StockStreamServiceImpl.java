package com.company.app.ordermanager.redis.stream.service.impl.stock;

import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.redis.stream.common.StreamFields;
import com.company.app.ordermanager.redis.stream.common.StreamNames;
import com.company.app.ordermanager.redis.stream.dto.StockUpdateMessage;
import com.company.app.ordermanager.redis.stream.service.api.stock.StockStreamService;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockStreamServiceImpl implements StockStreamService {
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    @Override
    public void requestStockReservation(UUID orderId, Set<OrderItem> orderItems) {
        Assert.notNull(orderId, "Order ID must not be null");
        Assert.notNull(orderItems, "Order items must not be null");

        try {
            List<StockUpdateMessage> stockUpdateMessages = orderItems.stream()
                    .map(item -> StockUpdateMessage.builder()
                            .orderId(orderId)
                            .orderItemId(item.getId())
                            .updateType(StockUpdateMessage.UpdateType.RESERVE)
                            .productId(item.getProduct().getId())
                            .quantity(item.getQuantity())
                            .build()
                    ).toList();

            List<StreamMessageId> messageIds = publishStockUpdateMessages(stockUpdateMessages);

            log.debug("Published order reservation messages with ID {} for order {}", messageIds, orderId);
        } catch (IllegalStateException e) {
            log.error("Failed to publish reservation messages for order {}. Error: {}", orderId, e.getMessage());
        }
    }

    @Override
    public void requestStockCancellation(UUID orderId, Set<OrderItem> orderItems) {
        Assert.notNull(orderId, "Order ID must not be null");
        Assert.notNull(orderItems, "Items must not be null");

        try {
            List<StockUpdateMessage> stockUpdateMessages = orderItems.stream()
                    .map(item -> StockUpdateMessage.builder()
                            .orderId(orderId)
                            .orderItemId(item.getId())
                            .updateType(StockUpdateMessage.UpdateType.CANCEL)
                            .productId(item.getProduct().getId())
                            .quantity(item.getQuantity())
                            .build()
                    )
                    .toList();

            List<StreamMessageId> messageIds = publishStockUpdateMessages(stockUpdateMessages);

            log.debug("Published order cancellation messages with IDs {} for order {}", messageIds, orderId);
        } catch (IllegalStateException e) {
            log.error("Failed to publish cancellation messages for order {}. Error: {}", orderId, e.getMessage());
        }
    }

    /**
     * Publishes a list of stock update messages to a Redis stream.
     * <p>
     * This method takes a list of {@link StockUpdateMessage} objects, serializes them into JSON format,
     * and publishes each message to a Redis stream identified by {@link StreamNames#STOCK_UPDATE}.
     * It returns the list of {@link StreamMessageId} objects corresponding to the messages successfully published.
     * </p>
     *
     * @param messages a {@link List} of {@link StockUpdateMessage} objects to be published.
     *                 Each message represents a stock update operation (e.g., reservation or cancellation).
     * @return a {@link List} of {@link StreamMessageId} objects representing the identifiers of the messages
     *         that were successfully published to the Redis stream.
     * @throws IllegalStateException if a message cannot be serialized to JSON format or if there is an error
     *                               during the publishing to the Redis stream.
     */
    private List<StreamMessageId> publishStockUpdateMessages(List<StockUpdateMessage> messages) {
        RStream<String, String> stream = redissonClient.getStream(StreamNames.STOCK_UPDATE.getKey());

        return messages.stream().map(message -> {
            String messageJson = null;
            try {
                messageJson = objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize stock update message", e);
            }

            return stream.add(StreamAddArgs.entry(StreamFields.MESSAGE.getField(), messageJson));
        }).toList();
    }


}
