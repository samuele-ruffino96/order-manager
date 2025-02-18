package com.company.app.ordermanager.service.api.orderitem;

import com.company.app.ordermanager.dto.orderitem.OrderItemDto;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;

import java.util.Set;
import java.util.UUID;

public interface OrderItemService {
    Set<OrderItemDto> cancelOrderItems(Set<UUID> orderItemIds);

    void updateOrderItemStatus(UUID orderItemId, OrderItemStatus status, long version);

    void updateOrderItemStatusAndReason(UUID orderItemId, OrderItemStatus status, long version, OrderItemStatusReason reason);
}
