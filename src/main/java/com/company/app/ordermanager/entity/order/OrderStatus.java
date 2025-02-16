package com.company.app.ordermanager.entity.order;

/**
 * Represents the various statuses an {@link Order} can have in the application.
 * <p>
 * This enumeration is used to track the lifecycle state of an {@link Order}.
 * The possible statuses are:
 * <ul>
 *   <li>{@link #PROCESSING}: Order items are being processed
 *   <li>{@link #CONFIRMED}: All items successfully processed
 *   <li>{@link #PARTIALLY_CONFIRMED}: Some items processed successfully, others failed
 *   <li>{@link #CANCELLED}: All items failed or order cancelled
 * </ul>
 */
public enum OrderStatus {
    PROCESSING,
    CONFIRMED,
    PARTIALLY_CONFIRMED,
    CANCELLED
}
