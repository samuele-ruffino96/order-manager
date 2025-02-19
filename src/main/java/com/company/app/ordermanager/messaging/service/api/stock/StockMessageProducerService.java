package com.company.app.ordermanager.messaging.service.api.stock;

import com.company.app.ordermanager.entity.orderitem.OrderItem;

import java.util.Set;

public interface StockMessageProducerService {
    void sendStockReservationMessage(Set<OrderItem> orderItems);

    void sendStockCancellationMessage(Set<OrderItem> orderItems);
}
