package com.company.app.ordermanager.redis.stream.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Represents a message used for stock updates such as reserving or canceling stock
 * for a specific product within an order. This class encapsulates the details of
 * the stock update operation.
 * <p>
 * The {@code StockUpdateMessage} includes the order ID, the type of update operation,
 * the product ID, and the quantity of stock being updated. The {@link UpdateType}
 * enumeration defines the available operations: {@code RESERVE} for reserving stock
 * and {@code CANCEL} for reverting a previous reservation.
 * </p>
 */
@Data
@Builder
public class StockUpdateMessage {
    private UUID orderId;
    private UpdateType updateType;
    private UUID productId;
    private int quantity;

    public enum UpdateType {
        RESERVE,
        CANCEL
    }
}
