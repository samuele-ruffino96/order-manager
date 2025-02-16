package com.company.app.ordermanager.service.api.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.dto.order.OrderDto;

public interface OrderService {
    OrderDto createOrder(CreateOrderDto createOrderDto);
}
