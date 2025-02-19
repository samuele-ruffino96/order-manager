package com.company.app.ordermanager.messaging.service.impl.stock.redis;

import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.messaging.common.MessageChannels;
import com.company.app.ordermanager.messaging.dto.StockUpdateMessage;
import com.company.app.ordermanager.messaging.redis.StreamFields;
import com.company.app.ordermanager.messaging.service.api.stock.StockMessageProducerService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamStockMessageProducer implements StockMessageProducerService {
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    /**
     * Sends stock reservation messages for the provided set of order items.
     * This method generates reservation messages for the given order items and publishes them
     * to a Redis stream for processing by downstream consumers.
     *
     * @param orderItems the set of {@link OrderItem} objects for which stock reservation messages should be sent
     * @throws IllegalArgumentException if the input set of order items is null
     */
    @Override
    public void sendStockReservationMessage(Set<OrderItem> orderItems) {
        Assert.notNull(orderItems, "Order items must not be null");

        List<StockUpdateMessage> stockUpdateMessages = orderItems.stream()
                .map(item -> StockUpdateMessage.builder()
                        .orderItemId(item.getId())
                        .expectedOrderItemVersion(item.getVersion())
                        .updateType(StockUpdateMessage.UpdateType.RESERVE)
                        .productId(item.getProduct().getId())
                        .quantity(item.getQuantity())
                        .build()
                ).toList();

        stockUpdateMessages.forEach(me -> {
            try {
                publishStockUpdateMessages(me);
            } catch (JsonProcessingException e) {
                log.error("Failed to publish reservation message for order item {}. Error: {}", me.getOrderItemId(), e.getMessage());
            }
        });
    }

    /**
     * Sends stock cancellation messages for the provided set of order items.
     * This method generates cancellation messages for the supplied order items and publishes them
     * to a Redis stream for further processing.
     *
     * @param orderItems the set of {@link OrderItem} objects for which stock cancellation messages should be sent
     * @throws IllegalArgumentException if the input set of order items is null
     */
    @Override
    public void sendStockCancellationMessage(Set<OrderItem> orderItems) {
        Assert.notNull(orderItems, "Items must not be null");

        List<StockUpdateMessage> stockUpdateMessages = orderItems.stream()
                .map(item -> StockUpdateMessage.builder()
                        .orderItemId(item.getId())
                        .expectedOrderItemVersion(item.getVersion())
                        .updateType(StockUpdateMessage.UpdateType.CANCEL)
                        .productId(item.getProduct().getId())
                        .quantity(item.getQuantity())
                        .build()
                )
                .toList();

        stockUpdateMessages.forEach(me -> {
            try {
                publishStockUpdateMessages(me);
            } catch (JsonProcessingException e) {
                log.error("Failed to publish cancellation message for order item {}. Error: {}", me.getOrderItemId(), e.getMessage());
            }
        });
    }

    /**
     * Publishes a stock update message to the stock update queue using the Redisson stream API.
     * This method serializes the provided {@link StockUpdateMessage} into JSON format and writes
     * it to a Redis stream for further processing.
     *
     * @param message the {@link StockUpdateMessage} containing stock update details
     * @return the {@link StreamMessageId} of the published message
     * @throws JsonProcessingException if the {@link ObjectMapper} fails to serialize the message
     */
    private StreamMessageId publishStockUpdateMessages(StockUpdateMessage message) throws JsonProcessingException {
        RStream<String, String> stream = redissonClient.getStream(MessageChannels.STOCK_UPDATE_QUEUE.getKey());

        String messageJson = objectMapper.writeValueAsString(message);

        StreamMessageId id = stream.add(StreamAddArgs.entry(StreamFields.MESSAGE.getField(), messageJson));

        log.debug("Published stock update message with ID {}. Message: {}", id, messageJson);

        return id;
    }
}
