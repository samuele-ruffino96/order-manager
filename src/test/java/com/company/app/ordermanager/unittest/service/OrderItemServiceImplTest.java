package com.company.app.ordermanager.unittest.service;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.messaging.service.api.stock.StockMessageProducerService;
import com.company.app.ordermanager.repository.api.orderitem.OrderItemRepository;
import com.company.app.ordermanager.service.api.product.ProductService;
import com.company.app.ordermanager.service.impl.orderitem.OrderItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {
    private static final UUID ORDER_ITEM_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private ProductService productService;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private StockMessageProducerService stockMessageProducerService;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private Order testOrder;
    private Product testProduct;
    private OrderItem testOrderItem;
    private CreateOrderItemDto testOrderItemDto;

    @BeforeEach
    void setUp() {
        // Create test entities
        testProduct = Product.builder()
                .id(PRODUCT_ID)
                .price(new BigDecimal("99.99"))
                .version(1L)
                .build();
        testOrder = Order.builder()
                .id(UUID.randomUUID())
                .customerName("Test Customer")
                .build();
        testOrderItem = OrderItem.builder()
                .id(ORDER_ITEM_ID)
                .order(testOrder)
                .product(testProduct)
                .quantity(2)
                .status(OrderItemStatus.PROCESSING)
                .version(1L)
                .build();

        testOrderItemDto = new CreateOrderItemDto();
        testOrderItemDto.setProductId(PRODUCT_ID);
        testOrderItemDto.setQuantity(2);
        testOrderItemDto.setProductVersion(1L);
    }

    @Test
    void createOrderItems_WhenValidInput_ShouldCreateAndReturnItems() {
        // Given
        Set<CreateOrderItemDto> dtos = Set.of(testOrderItemDto);
        when(productService.findAllById(Set.of(PRODUCT_ID))).thenReturn(Set.of(testProduct));
        when(orderItemRepository.saveAll(any())).thenReturn(List.of(testOrderItem));

        // When
        Set<OrderItem> result = orderItemService.createOrderItems(testOrder, dtos);

        // Then
        assertThat(result).hasSize(1);
        OrderItem createdItem = result.iterator().next();
        assertThat(createdItem.getProduct()).isEqualTo(testProduct);
        assertThat(createdItem.getOrder()).isEqualTo(testOrder);
        assertThat(createdItem.getQuantity()).isEqualTo(testOrderItemDto.getQuantity());
    }

    @Test
    void createOrderItems_WhenProductNotFound_ShouldThrowException() {
        // Given
        Set<CreateOrderItemDto> dtos = Set.of(testOrderItemDto);
        when(productService.findAllById(Set.of(PRODUCT_ID))).thenReturn(Set.of());

        // When/Then
        assertThrows(ProductNotFoundException.class, () ->
                orderItemService.createOrderItems(testOrder, dtos)
        );
    }

    @Test
    void cancelOrderItems_WhenItemsExist_ShouldCancelAndNotifyStock() {
        // Given
        Set<UUID> itemIds = Set.of(ORDER_ITEM_ID);
        when(orderItemRepository.findAllById(itemIds)).thenReturn(List.of(testOrderItem));
        when(orderItemRepository.saveAll(any())).thenReturn(List.of(testOrderItem));

        // When
        Set<OrderItem> result = orderItemService.cancelOrderItems(itemIds);

        // Then
        assertThat(result).hasSize(1);
        OrderItem cancelledItem = result.iterator().next();
        assertThat(cancelledItem.getStatus()).isEqualTo(OrderItemStatus.CANCELLING);
        assertThat(cancelledItem.getReason()).isEqualTo(OrderItemStatusReason.USER_CANCELLED);
        verify(stockMessageProducerService).sendStockCancellationMessage(result);
    }

    @Test
    void updateOrderItemStatus_WhenSuccessful_ShouldUpdateStatus() {
        // Given
        when(orderItemRepository.updateStatus(ORDER_ITEM_ID, OrderItemStatus.CONFIRMED, 1L))
                .thenReturn(1);

        // When
        orderItemService.updateOrderItemStatus(ORDER_ITEM_ID, OrderItemStatus.CONFIRMED, 1L);

        // Then
        verify(orderItemRepository).updateStatus(ORDER_ITEM_ID, OrderItemStatus.CONFIRMED, 1L);
    }

    @Test
    void updateOrderItemStatusAndReason_WhenSuccessful_ShouldUpdateStatusAndReason() {
        // Given
        when(orderItemRepository.updateStatusAndReason(
                ORDER_ITEM_ID,
                OrderItemStatus.CANCELLED,
                1L,
                OrderItemStatusReason.INSUFFICIENT_STOCK
        )).thenReturn(1);

        // When
        orderItemService.updateOrderItemStatusAndReason(
                ORDER_ITEM_ID,
                OrderItemStatus.CANCELLED,
                1L,
                OrderItemStatusReason.INSUFFICIENT_STOCK
        );

        // Then
        verify(orderItemRepository).updateStatusAndReason(
                ORDER_ITEM_ID,
                OrderItemStatus.CANCELLED,
                1L,
                OrderItemStatusReason.INSUFFICIENT_STOCK
        );
    }
}
