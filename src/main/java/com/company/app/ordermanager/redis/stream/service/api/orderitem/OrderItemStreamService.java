package com.company.app.ordermanager.redis.stream.service.api.orderitem;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;

import java.util.UUID;

public interface OrderItemStreamService {
    /**
     * Sends a status update for a specific order item within a given order.
     *
     * @param orderId     The unique identifier of the order to which the order item belongs. Must not be {@literal null}.
     * @param orderItemId The unique identifier of the order item whose status is being updated. Must not be {@literal null}.
     * @param productId   The unique identifier of the product to which the order item belongs. Must not be {@literal null}.
     * @param newStatus   The new status for the order item. This is represented by an instance of {@link OrderItemStatus}. Must not be {@literal null}.
     * @throws IllegalArgumentException If any of the provided parameters {@code orderId}, {@code orderItemId}, or {@code newStatus} is {@literal null}.
     */
    void sendOrderItemStatusUpdateMessage(UUID orderId, UUID orderItemId, UUID productId, OrderItemStatus newStatus);

    /**
     * Sends a status update for a specific order item within a given order.
     *
     * @param orderId     The unique identifier of the order to which the order item belongs. Must not be {@literal null}.
     * @param orderItemId The unique identifier of the order item whose status is being updated. Must not be {@literal null}.
     * @param productId   The unique identifier of the product to which the order item belongs. Must not be {@literal null}.
     * @param newStatus   The new status for the order item. This is represented by an instance of {@link OrderItemStatus}. Must not be {@literal null}.
     * @param error       The error message to include within the message.
     * @throws IllegalArgumentException If any of the provided parameters {@code orderId}, {@code orderItemId}, or {@code newStatus} is {@literal null}.
     */
    void sendOrderItemStatusUpdateMessage(UUID orderId, UUID orderItemId, UUID productId, OrderItemStatus newStatus, String error);
}
