package com.company.app.ordermanager.redis.stream.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Represents a stock update message containing information crucial for updating stock levels
 * in inventory systems. The message includes details such as order identification,
 * item identification, type of update, product identification, and quantity.
 *
 * This class is typically used to communicate stock reservation or cancellation
 * instructions to inventory systems or services.
 */
@Data
@Builder
public class StockUpdateMessage {
    private UUID orderId;
    private UUID orderItemId;
    private UpdateType updateType;
    private UUID productId;
    private int quantity;

    public enum UpdateType {
        RESERVE,
        CANCEL
    }
}
