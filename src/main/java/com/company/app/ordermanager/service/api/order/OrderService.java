package com.company.app.ordermanager.service.api.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.exception.order.OrderNotFoundException;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.product.ProductVersionMismatchException;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    /**
     * Retrieves a pageable list of {@link Order} entities that match the given {@link Predicate}.
     *
     * @param predicate the condition to filter orders.
     * @param pageable  the pagination and sorting information.
     * @return a page of matching {@link Order} entities.
     * @throws IllegalArgumentException if predicate or pageable is null.
     */
    Page<Order> findAll(Predicate predicate, Pageable pageable);

    /**
     * Finds an {@link Order} entity by its unique identifier.
     *
     * @param id the unique identifier of the {@link Order}.
     * @return the {@link Order} entity associated with the given identifier.
     * @throws IllegalArgumentException if the provided id is null.
     * @throws OrderNotFoundException   if no {@link Order} is found for the given identifier.
     */
    Order findById(UUID id);

    /**
     * Deletes an {@link Order} identified by its unique identifier and cancels its associated items.
     *
     * @param id the unique identifier of the {@link Order} to be deleted.
     * @throws IllegalArgumentException if the provided id is null.
     * @throws OrderNotFoundException   if no {@link Order} is found with the given id.
     */
    void deleteById(UUID id);

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
    Order createOrder(CreateOrderDto createOrderDto);
}
