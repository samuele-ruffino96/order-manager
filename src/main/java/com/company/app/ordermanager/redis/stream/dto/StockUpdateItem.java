package com.company.app.ordermanager.redis.stream.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Represents an individual stock update item associated with a stock update operation.
 * This class is used to specify a product and the quantity associated with a stock reservation
 * or cancellation {@link StockUpdateMessage}.
 */
@Data
@Builder
public class StockUpdateItem {
    private UUID productId;
    private int quantity;
}
