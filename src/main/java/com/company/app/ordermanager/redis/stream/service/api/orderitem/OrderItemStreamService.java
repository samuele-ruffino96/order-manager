package com.company.app.ordermanager.redis.stream.service.api.orderitem;

import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;

import java.util.UUID;

public interface OrderItemStreamService {
    void requestOrderItemStatusUpdate(UUID orderId, UUID orderItemId, OrderItemStatus newStatus);

    void requestOrderItemStatusUpdate(UUID orderId, UUID orderItemId, OrderItemStatus newStatus, String error);
}
