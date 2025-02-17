package com.company.app.ordermanager.redis.stream.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a stock update request message sent to a Redis stream, containing information
 * about an order, the type of stock update operation, and the associated items.
 */
@Data
@Builder
public class StockUpdateMessage {
    private UUID orderId;
    private UpdateType updateType;
    private Set<StockUpdateItem> items;

    public enum UpdateType {
        RESERVE,
        CANCEL
    }
}
