package com.company.app.ordermanager.service.api.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.dto.order.OrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;

public interface OrderService {
    /**
     * Handles the creation of a new order based on the provided {@link CreateOrderDto}.
     * This method performs order creation, validation of products, and sends a stock reservation request.
     * <p>
     * The creation process involves:
     * <ul>
     *     <li>Retrieving the products required for the order and validating their existence.</li>
     *     <li>Verifying product version to ensure data consistency.</li>
     *     <li>Building and saving the {@link Order} and its associated {@link OrderItem}s.</li>
     *     <li>Sending a stock reservation request for the order items.</li>
     * </ul>
     * </p>
     *
     * @param createOrderDto {@link CreateOrderDto} containing the order details, including customer information,
     *                       description, and a list of items to be ordered. Must not be {@code null}.
     * @return {@link OrderDto} representing the created order along with its details.
     * @throws IllegalArgumentException if the {@code createOrderDto} is {@code null} or the {@code productIds}
     *                                  in the {@code createOrderDto} do not all exist in the database.</li>
     * @throws IllegalStateException    if there is a mismatch between the provided product version and the actual product version in the database.</li>
     */
    OrderDto createOrder(CreateOrderDto createOrderDto);
}
