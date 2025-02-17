package com.company.app.ordermanager.redis.stream.dto;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OrderItemStatusUpdateMessage {
    private UUID orderId;
    private UUID orderItemId;
    private UUID productId;
    private OrderItemStatus newStatus;
    private String error;
}
