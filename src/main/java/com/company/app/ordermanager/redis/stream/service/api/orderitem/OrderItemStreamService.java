package com.company.app.ordermanager.redis.stream.service.api.orderitem;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;

import java.util.UUID;

public interface OrderItemStreamService {
    void sendOrderItemStatusUpdateMessage(UUID orderId, UUID orderItemId, OrderItemStatus newStatus);

    void sendOrderItemStatusUpdateMessage(UUID orderId, UUID orderItemId, OrderItemStatus newStatus, String error);
}
