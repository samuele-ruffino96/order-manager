package com.company.app.ordermanager.unittest.service;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.order.QOrder;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.order.OrderNotFoundException;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.messaging.service.api.stock.StockMessageProducerService;
import com.company.app.ordermanager.repository.api.order.OrderRepository;
import com.company.app.ordermanager.service.api.orderitem.OrderItemService;
import com.company.app.ordermanager.service.impl.order.OrderServiceImpl;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private StockMessageProducerService stockMessageProducerService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private CreateOrderDto createOrderDto;

    @BeforeEach
    void setUp() {
        // Create test entities
        Product testProduct = Product.builder()
                .id(UUID.randomUUID())
                .price(new BigDecimal("99.99"))
                .version(1L)
                .build();
        testOrder = Order.builder()
                .id(ORDER_ID)
                .customerName("Test Customer")
                .description("Test Order")
                .build();
        testOrderItem = OrderItem.builder()
                .order(testOrder)
                .product(testProduct)
                .quantity(2)
                .status(OrderItemStatus.PROCESSING)
                .version(1L)
                .build();

        createOrderDto = new CreateOrderDto();
        createOrderDto.setCustomerName("Test Customer");
        createOrderDto.setDescription("Test Order");

        CreateOrderItemDto itemDto = new CreateOrderItemDto();
        itemDto.setProductId(testProduct.getId());
        itemDto.setQuantity(2);
        createOrderDto.setItems(Set.of(itemDto));
    }

    @Test
    void findAll_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Predicate predicate = QOrder.order.customerName.eq("Test");
        Page<Order> expectedPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findAll(predicate, pageable)).thenReturn(expectedPage);

        // When
        Page<Order> result = orderService.findAll(predicate, pageable);

        // Then
        assertThat(result.getContent()).containsExactly(testOrder);
    }

    @Test
    void findById_WhenOrderExists_ShouldReturnOrder() {
        // Given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        // When
        Order result = orderService.findById(ORDER_ID);

        // Then
        assertThat(result).isEqualTo(testOrder);
    }

    @Test
    void findById_WhenOrderDoesNotExist_ShouldThrowException() {
        // Given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(OrderNotFoundException.class, () ->
                orderService.findById(ORDER_ID)
        );
    }

    @Test
    void deleteById_WhenOrderExists_ShouldCancelItems() {
        // Given
        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .status(OrderItemStatus.PROCESSING)
                .build();
        testOrder.setOrderItems(Set.of(item));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        // When
        orderService.deleteById(ORDER_ID);

        // Then
        verify(orderItemService).cancelOrderItems(Set.of(item.getId()));
    }

    @Test
    void deleteById_WhenOrderDoesNotExist_ShouldThrowException() {
        // Given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(OrderNotFoundException.class, () ->
                orderService.deleteById(ORDER_ID)
        );
        verify(orderItemService, never()).cancelOrderItems(any());
    }

    @Test
    void createOrder_WhenValidInput_ShouldCreateOrderAndItems() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderItemService.createOrderItems(any(), any())).thenReturn(Set.of(testOrderItem));

        // When
        Order result = orderService.createOrder(createOrderDto);

        // Then
        assertThat(result.getCustomerName()).isEqualTo(createOrderDto.getCustomerName());
        assertThat(result.getDescription()).isEqualTo(createOrderDto.getDescription());
        verify(stockMessageProducerService).sendStockReservationMessage(any());
    }

    @Test
    void createOrder_WhenProductNotFound_ShouldThrowException() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderItemService.createOrderItems(any(), any()))
                .thenThrow(new ProductNotFoundException(UUID.randomUUID()));

        // When/Then
        assertThrows(ProductNotFoundException.class, () ->
                orderService.createOrder(createOrderDto)
        );
    }
}
