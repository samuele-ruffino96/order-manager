package com.company.app.ordermanager.service.api.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    Page<Order> findAll(Predicate predicate, Pageable pageable);

    Order findById(UUID id);

    void deleteById(UUID id);

    Order createOrder(CreateOrderDto createOrderDto);
}
