package com.company.app.ordermanager.service.api.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    Page<Order> findAll(Predicate predicate, Pageable pageable);

    Order findById(UUID id);

    void deleteById(UUID id);

    /**
     * Handles the creation of a new order based on the provided {@link CreateOrderDto}.
     * This method performs order creation, validation of products, and sends a stock reservation request.
     *
     * @param createOrderDto {@link CreateOrderDto} containing the order details, including customer information,
     *                       description, and a list of items to be ordered. Must not be {@code null}.
     * @return {@link Order} representing the created order along with its details.
     * @throws IllegalArgumentException if the {@code createOrderDto} is {@code null}
     * @throws ProductNotFoundException if the {@code productIds} in the {@code createOrderDto} do not all exist in the database.
     * @throws IllegalStateException    if there is a mismatch between the provided product version and the actual product version in the database.</li>
     */
    Order createOrder(CreateOrderDto createOrderDto);
}
