package com.company.app.ordermanager.entity.order;

/**
 * Represents the various statuses an {@link Order} can have in the application.
 * <p>
 * This enumeration is used to track the lifecycle state of an {@link Order}.
 * The possible statuses are:
 * <ul>
 *   <li>{@link #PENDING}: The order has been created but not yet processed.</li>
 *   <li>{@link #CONFIRMED}: The order has been approved and is being processed.</li>
 *   <li>{@link #CANCELLED}: The order has been cancelled and will not be processed further.</li>
 * </ul>
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
