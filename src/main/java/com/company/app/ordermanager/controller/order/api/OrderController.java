package com.company.app.ordermanager.controller.order.api;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

public interface OrderController {
    Page<Order> getOrdersList(@QuerydslPredicate(root = Order.class) Predicate predicate, Pageable pageable);

    Order getOrderById(@PathVariable("id") UUID id);

    Order createOrder(@Valid @RequestBody CreateOrderDto order);

    void deleteOrderById(@PathVariable("id") UUID id);
}
