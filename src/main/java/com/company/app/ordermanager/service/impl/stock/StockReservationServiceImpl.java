package com.company.app.ordermanager.service.impl.stock;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.redis.stream.common.StreamFields;
import com.company.app.ordermanager.redis.stream.common.StreamNames;
import com.company.app.ordermanager.redis.stream.dto.StockUpdateMessage;
import com.company.app.ordermanager.service.api.stock.StockReservationService;
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

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationServiceImpl implements StockReservationService {
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    @Override
    public void requestStockReservation(UUID orderId, Set<CreateOrderItemDto> items) {
        Assert.notNull(orderId, "Order ID must not be null");
        Assert.notNull(items, "Items must not be null");

        try {
            StreamMessageId messageId = publishStockUpdateMessage(orderId, items, StockUpdateMessage.UpdateType.RESERVE);

            log.debug("Published order reservation message with ID {} for order {}", messageId, orderId);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish reservation message for order {}. Error: {}", orderId, e.getMessage());
        }
    }

    @Override
    public void requestStockCancellation(UUID orderId, Set<CreateOrderItemDto> items) {
        Assert.notNull(orderId, "Order ID must not be null");
        Assert.notNull(items, "Items must not be null");

        try {
            StreamMessageId messageId = publishStockUpdateMessage(orderId, items, StockUpdateMessage.UpdateType.CANCEL);

            log.debug("Published order cancellation message with ID {} for order {}", messageId, orderId);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish cancellation message for order {}. Error: {}", orderId, e.getMessage());
        }
    }

    /**
     * Publishes a stock update message to a Redis stream. The message contains information about a specific order,
     * the type of update (e.g., reserve or cancel), and the items involved in the operation. This method constructs
     * a {@link StockUpdateMessage}, serializes it to JSON, and sends it to the appropriate Redis stream.
     *
     * @param orderId    the unique identifier of the order for which the stock update is being performed.
     * @param items      the set of items involved in the stock update.
     * @param updateType the type of update being performed.
     * @return the {@link StreamMessageId} of the added message in the Redis stream.
     * @throws JsonProcessingException if there is an issue serializing the {@link StockUpdateMessage} to JSON string.
     */
    private StreamMessageId publishStockUpdateMessage(UUID orderId, Set<CreateOrderItemDto> items, StockUpdateMessage.UpdateType updateType) throws JsonProcessingException {
        RStream<String, String> stream = redissonClient.getStream(StreamNames.STOCK_UPDATE.getKey());

        StockUpdateMessage message = StockUpdateMessage.builder()
                .orderId(orderId)
                .updateType(updateType)
                .items(items)
                .build();

        String messageJson = objectMapper.writeValueAsString(message);

        return stream.add(StreamAddArgs.entry(StreamFields.MESSAGE.getField(), messageJson));
    }
}
