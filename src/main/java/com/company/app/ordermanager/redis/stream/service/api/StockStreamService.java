package com.company.app.ordermanager.redis.stream.service.api;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.orderitem.OrderItem;

import java.util.Set;
import java.util.UUID;

/**
 * The {@code StockStreamService} interface defines operations for managing stock reservations and cancellations
 * for a given order. It serves as a bridge to communicate stock update requests to the stock management system
 * using messaging systems like Redis streams.
 */
public interface StockStreamService {
    /**
     * Perform stock reservation requests for a given order. This method prepares and publishes a reservation message
     * to a Redis stream to inform the stock management system to reserve stock for the specified order items.
     *
     * @param orderId the unique identifier of the order for which stock reservation is being requested.
     *                This parameter must not be {@literal null}.
     * @param items   the set of {@link CreateOrderItemDto} objects representing the order items for which stock
     *                should be reserved. This parameter must not be {@literal null}.
     * @throws IllegalArgumentException if the {@code orderId} or {@code items} parameter is {@literal null}.
     */
    void requestStockReservation(UUID orderId, Set<CreateOrderItemDto> items);

    /**
     * Perform stock cancellation requests for a given order. This method prepares and publishes a cancellation message
     * to a Redis stream to informs the stock management system to cancel stock reservation for the specified order items.
     *
     * @param orderId the unique identifier of the order for which stock cancellation is being requested.
     *                This parameter must not be {@literal null}.
     * @param items   the set of {@link OrderItem} object in the order that need to be canceled.
     *                This parameter must not be {@literal null}.
     * @throws IllegalArgumentException if the {@code orderId} or {@code items} parameter is {@literal null}.
     */
    void requestStockCancellation(UUID orderId, Set<OrderItem> items);
}
