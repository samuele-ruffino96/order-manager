package com.company.app.ordermanager.service.impl.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.exception.order.OrderNotFoundException;
import com.company.app.ordermanager.redis.stream.service.api.stock.StockStreamService;
import com.company.app.ordermanager.repository.api.order.OrderRepository;
import com.company.app.ordermanager.service.api.order.OrderService;
import com.company.app.ordermanager.service.api.orderitem.OrderItemService;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemService orderItemService;
    private final StockStreamService stockStreamService;

    @Override
    public Page<Order> findAll(Predicate predicate, Pageable pageable) {
        Assert.notNull(predicate, "Predicate must not be null");
        Assert.notNull(pageable, "Pageable must not be null");

        return orderRepository.findAll(predicate, pageable);
    }

    @Override
    public Order findById(UUID id) {
        Assert.notNull(id, "Order ID must not be null");

        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public void deleteById(UUID id) {
        Assert.notNull(id, "Order ID must not be null");

        Order order = findById(id);

        Set<UUID> orderItemIds = order.getOrderItems().stream().map(OrderItem::getId).collect(Collectors.toSet());

        orderItemService.cancelOrderItems(orderItemIds);
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderDto createOrderDto) {
        Assert.notNull(createOrderDto, "Create order DTO must not be null");

        // Create order
        Order order = Order.builder()
                .customerName(createOrderDto.getCustomerName())
                .description(createOrderDto.getDescription())
                .build();

        // Save the order and its items
        Order savedOrder = orderRepository.save(order);

        // Create order items
        Set<OrderItem> orderItems = orderItemService.createOrderItems(savedOrder, createOrderDto.getItems());

        // Set order items
        order.setOrderItems(orderItems);

        // Send stock reservation request to queue
        stockStreamService.sendStockReservationMessage(savedOrder.getOrderItems());

        return savedOrder;
    }
}
