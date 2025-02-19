package com.company.app.ordermanager.messaging.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Represents a message detailing a stock update operation, such as reserving or canceling
 * stock for a specific product and order item. This class serves as a data transfer object
 * encapsulating all necessary details for such operations.
 */
@Data
@Builder
public class StockUpdateMessage {
    private UUID orderId;
    private UUID orderItemId;
    private long expectedOrderItemVersion;
    private UpdateType updateType;
    private UUID productId;
    private int quantity;

    public enum UpdateType {
        RESERVE,
        CANCEL
    }
}
