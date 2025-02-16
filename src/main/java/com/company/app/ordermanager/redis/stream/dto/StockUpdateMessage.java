package com.company.app.ordermanager.redis.stream.dto;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a message used for updating stock reservations or cancellations in the order management system.
 * This message contains the {@code orderId}, the type of stock update, and the set of items involved in the operation.
 */
@Data
@Builder
public class StockUpdateMessage {
    private UUID orderId;
    private UpdateType updateType;
    private Set<CreateOrderItemDto> items;

    public enum UpdateType {
        RESERVE,
        CANCEL
    }
}
