package com.company.app.ordermanager.entity.orderitem;

/**
 * Represents the status of an {@link OrderItem}.
 * <p>
 * The {@code OrderItemStatus} enum defines the possible states in which an {@link OrderItem}
 * can be during its lifecycle, such as reserving stock, successful reservation, or failure.
 * </p>
 * <p>Possible statuses include:</p>
 * <ul>
 *   <li>{@code PENDING}: Indicates that the system is attempting to reserve the required stock for the item.</li>
 *   <li>{@code FAILED}: Indicates that the stock reservation for the item failed, often accompanied by an error message.</li>
 *   <li>{@code COMPLETED}: Indicates that the stock for the item was successfully reserved.</li>
 * </ul>
 */
public enum OrderItemStatus {
    PENDING,         // Attempting to reserve stock
    FAILED,          // Processing failed (with error message)
    COMPLETED        // Stock successfully reserved
}
