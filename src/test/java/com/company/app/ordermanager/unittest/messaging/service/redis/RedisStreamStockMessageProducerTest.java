package com.company.app.ordermanager.unittest.messaging.service.redis;

import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.messaging.common.MessageChannels;
import com.company.app.ordermanager.messaging.dto.StockUpdateMessage;
import com.company.app.ordermanager.messaging.service.impl.stock.redis.RedisStreamStockMessageProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisStreamStockMessageProducerTest {
    private static final UUID ORDER_ITEM_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RStream<Object, Object> stream;

    @InjectMocks
    private RedisStreamStockMessageProducer producer;

    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        // Create test entities
        Product testProduct = Product.builder()
                .id(PRODUCT_ID)
                .version(1L)
                .build();
        testOrderItem = OrderItem.builder()
                .id(ORDER_ITEM_ID)
                .product(testProduct)
                .quantity(2)
                .version(1L)
                .build();

        when(redissonClient.getStream(MessageChannels.STOCK_UPDATE_QUEUE.getKey())).thenReturn(stream);
    }

    @Test
    void sendStockReservationMessage_ShouldPublishToStream() throws JsonProcessingException {
        // Given
        StockUpdateMessage expectedMessage = StockUpdateMessage.builder()
                .orderItemId(ORDER_ITEM_ID)
                .expectedOrderItemVersion(1L)
                .updateType(StockUpdateMessage.UpdateType.RESERVE)
                .productId(PRODUCT_ID)
                .quantity(2)
                .build();

        String messageJson = "message-json";
        when(objectMapper.writeValueAsString(expectedMessage)).thenReturn(messageJson);

        // When
        producer.sendStockReservationMessage(Set.of(testOrderItem));

        // Then
        verify(stream).add(any(StreamAddArgs.class));
    }

    @Test
    void sendStockCancellationMessage_ShouldPublishToStream() throws JsonProcessingException {
        // Given
        StockUpdateMessage expectedMessage = StockUpdateMessage.builder()
                .orderItemId(ORDER_ITEM_ID)
                .expectedOrderItemVersion(1L)
                .updateType(StockUpdateMessage.UpdateType.CANCEL)
                .productId(PRODUCT_ID)
                .quantity(2)
                .build();

        String messageJson = "message-json";
        when(objectMapper.writeValueAsString(expectedMessage)).thenReturn(messageJson);

        // When
        producer.sendStockCancellationMessage(Set.of(testOrderItem));

        // Then
        verify(stream).add(any(StreamAddArgs.class));
    }
}
