package com.company.app.ordermanager.entity.orderitem;

/**
 * Enumerates the reasons for the status of an {@link OrderItem}.
 * <p>
 * The {@code OrderItemStatusReason} enum provides detailed explanations for why an
 * {@link OrderItem} has a specific {@link OrderItemStatus}. These reasons are particularly
 * useful in cases of failure or cancellation, offering insight into statuses such as
 * "insufficient stock" or "user-initiated cancellation".
 */
public enum OrderItemStatusReason {
    INSUFFICIENT_STOCK,
    USER_CANCELLED,
}
