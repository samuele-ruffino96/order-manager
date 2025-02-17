package com.company.app.ordermanager.entity.orderitem;

/**
 * Represents the status of an {@link OrderItem}.
 * <p>
 * The {@code OrderItemStatus} enum defines the possible states in which an {@link OrderItem}
 * can be during its lifecycle, such as reserving stock, successful reservation, or failure.
 * </p>
 * <p>Possible statuses include:</p>
 * <ul>
 *   <li>{@link #PROCESSING}: Indicates that the system is attempting to reserve the required stock for the item.</li>
 *   <li>{@link #PROCESSING_FAILED}: Indicates that the stock reservation for the item failed, often accompanied by an error message.</li>
 *   <li>{@link #CONFIRMED}: Indicates that the stock for the item was successfully reserved.</li>
 *   <li>{@link #CANCELLING}: Indicates that the system is attempting to release stock for and cancel the order item.</li>
 *   <li>{@link #CANCELLED}: Indicates the stock for the item was successfully released.</li>
 * </ul>
 */
public enum OrderItemStatus {
    PROCESSING,
    PROCESSING_FAILED,
    CONFIRMED,
    CANCELLING,
    CANCELLED
}
