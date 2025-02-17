package com.company.app.ordermanager.service.api.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.dto.order.OrderDto;

import java.util.UUID;

public interface OrderService {
    OrderDto createOrder(CreateOrderDto createOrderDto);

    void cancelOrderItem(UUID orderId, UUID orderItemId);
}
