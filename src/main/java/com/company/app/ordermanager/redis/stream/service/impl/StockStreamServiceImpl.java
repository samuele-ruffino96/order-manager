package com.company.app.ordermanager.redis.stream.service.impl;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.redis.stream.common.StreamFields;
import com.company.app.ordermanager.redis.stream.common.StreamNames;
import com.company.app.ordermanager.redis.stream.dto.StockUpdateItem;
import com.company.app.ordermanager.redis.stream.dto.StockUpdateMessage;
import com.company.app.ordermanager.redis.stream.service.api.StockStreamService;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockStreamServiceImpl implements StockStreamService {
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    @Override
    public void requestStockReservation(UUID orderId, Set<CreateOrderItemDto> createOrderItemDtos) {
        Assert.notNull(orderId, "Order ID must not be null");
        Assert.notNull(createOrderItemDtos, "Items must not be null");

        try {
            Set<StockUpdateItem> items = createOrderItemDtos.stream()
                    .map(item -> StockUpdateItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build()
                    )
                    .collect(Collectors.toSet());

            List<StreamMessageId> messageIds = publishStockUpdateMessages(orderId, items, StockUpdateMessage.UpdateType.RESERVE);

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
            Set<StockUpdateItem> items = orderItems.stream()
                    .map(item -> StockUpdateItem.builder()
                            .productId(item.getProduct().getId())
                            .quantity(item.getQuantity())
                            .build()
                    )
                    .collect(Collectors.toSet());

            List<StreamMessageId> messageIds = publishStockUpdateMessages(orderId, items, StockUpdateMessage.UpdateType.CANCEL);

            log.debug("Published order cancellation messages with IDs {} for order {}", messageIds, orderId);
        } catch (IllegalStateException e) {
            log.error("Failed to publish cancellation messages for order {}. Error: {}", orderId, e.getMessage());
        }
    }

    /**
     * Responsible for publishing stock update messages to a Redis stream for processing.
     * The method processes a set of stock update items related to an order and sends
     * them to the appropriate Redis stream.
     *
     * @param orderId    the unique identifier of the order for which the stock update is being performed.
     * @param items      the set of items involved in the stock update.
     * @param updateType the type of update being performed.
     * @return A <code>List</code> of {@link StreamMessageId} representing the identifiers
     * of the messages pushed to the Redis stream.
     * @throws IllegalStateException If an error occurs during the serialization of any
     *                                 {@link StockUpdateMessage} objects into JSON strings.
     */
    private List<StreamMessageId> publishStockUpdateMessages(UUID orderId, Set<StockUpdateItem> items, StockUpdateMessage.UpdateType updateType) {
        RStream<String, String> stream = redissonClient.getStream(StreamNames.STOCK_UPDATE.getKey());

        return items.stream().map(item -> {
            StockUpdateMessage message = StockUpdateMessage.builder()
                    .orderId(orderId)
                    .updateType(updateType)
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build();

            String messageJson = null;
            try {
                messageJson = objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize stock update message", e);
            }

            return stream.add(StreamAddArgs.entry(StreamFields.MESSAGE.getField(), messageJson));
        }).toList();
    }

    /**
     * Publishes a stock update message to a Redis stream. The message contains information about a specific order,
     * the type of update (e.g., reserve or cancel), and the items involved in the operation. This method constructs
     * a {@link StockUpdateMessage}, serializes it to JSON, and sends it to the appropriate Redis stream.
     *
     * @param orderId    the unique identifier of the order for which the stock update is being performed.
     * @param items      the set of items involved in the stock update.
     * @param updateType the type of update being performed.
     * @return the {@link List<StreamMessageId>} of the added message in the Redis stream.
     * @throws JsonProcessingException if there is an issue serializing the {@link StockUpdateMessage} to JSON string.
     */
}
