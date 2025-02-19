package com.company.app.ordermanager.service.api.orderitem;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;

import java.util.Set;
import java.util.UUID;

public interface OrderItemService {
    Set<OrderItem> createOrderItems(Order order, Set<CreateOrderItemDto> orderItemDtos);

    Set<OrderItem> cancelOrderItems(Set<UUID> orderItemIds);

    void updateOrderItemStatus(UUID orderItemId, OrderItemStatus status, long orderItemVersion);

    void updateOrderItemStatusAndReason(UUID orderItemId, OrderItemStatus status, long version, OrderItemStatusReason reason);
}
