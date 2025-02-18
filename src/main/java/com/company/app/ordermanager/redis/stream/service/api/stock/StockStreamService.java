package com.company.app.ordermanager.redis.stream.service.api.stock;

import com.company.app.ordermanager.entity.orderitem.OrderItem;

import java.util.Set;

public interface StockStreamService {
    /**
     * Sends stock reservation messages for the provided set of order items.
     * This method generates reservation messages for the given order items and publishes them
     * to a Redis stream for processing by downstream consumers.
     *
     * @param orderItems the set of {@link OrderItem} objects for which stock reservation messages should be sent
     * @throws IllegalArgumentException if the input set of order items is null
     */
    void sendStockReservationMessage(Set<OrderItem> orderItems);

    /**
     * Sends stock cancellation messages for the provided set of order items.
     * This method generates cancellation messages for the supplied order items and publishes them
     * to a Redis stream for further processing.
     *
     * @param orderItems the set of {@link OrderItem} objects for which stock cancellation messages should be sent
     * @throws IllegalArgumentException if the input set of order items is null
     */
    void sendStockCancellationMessage(Set<OrderItem> orderItems);
}
