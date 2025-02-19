package com.company.app.ordermanager.service.impl.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.exception.order.OrderNotFoundException;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.product.ProductVersionMismatchException;
import com.company.app.ordermanager.messaging.service.api.stock.StockMessageProducerService;
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
    private final StockMessageProducerService stockMessageProducerService;

    /**
     * Retrieves a pageable list of {@link Order} entities that match the given {@link Predicate}.
     *
     * @param predicate the condition to filter orders.
     * @param pageable  the pagination and sorting information.
     * @return a page of matching {@link Order} entities.
     * @throws IllegalArgumentException if predicate or pageable is null.
     */
    @Override
    public Page<Order> findAll(Predicate predicate, Pageable pageable) {
        Assert.notNull(predicate, "Predicate must not be null");
        Assert.notNull(pageable, "Pageable must not be null");

        return orderRepository.findAll(predicate, pageable);
    }

    /**
     * Finds an {@link Order} entity by its unique identifier.
     *
     * @param id the unique identifier of the {@link Order}.
     * @return the {@link Order} entity associated with the given identifier.
     * @throws IllegalArgumentException if the provided id is null.
     * @throws OrderNotFoundException   if no {@link Order} is found for the given identifier.
     */
    @Override
    public Order findById(UUID id) {
        Assert.notNull(id, "Order ID must not be null");

        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Deletes an {@link Order} identified by its unique identifier and cancels its associated items.
     *
     * @param id the unique identifier of the {@link Order} to be deleted.
     * @throws IllegalArgumentException if the provided id is null.
     * @throws OrderNotFoundException   if no {@link Order} is found with the given id.
     */
    @Override
    public void deleteById(UUID id) {
        Assert.notNull(id, "Order ID must not be null");

        Order order = findById(id);

        Set<UUID> orderItemIds = order.getOrderItems().stream().map(OrderItem::getId).collect(Collectors.toSet());

        orderItemService.cancelOrderItems(orderItemIds);
    }

    /**
     * Creates a new {@link Order} entity using the provided order details and initializes
     * associated order items, while triggering stock reservation messaging.
     *
     * @param createOrderDto the data transfer object containing details of the order to be created.
     * @return the newly created {@link Order} entity.
     * @throws IllegalArgumentException        if createOrderDto is null.
     * @throws ProductNotFoundException        if any referenced products in orderItemDtos are not found.
     * @throws ProductVersionMismatchException if the product versions in orderItemDtos do not match the current product versions.
     */
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
        stockMessageProducerService.sendStockReservationMessage(savedOrder.getOrderItems());

        return savedOrder;
    }
}
