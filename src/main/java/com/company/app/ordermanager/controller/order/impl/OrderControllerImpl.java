package com.company.app.ordermanager.controller.order.impl;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.service.api.order.OrderService;
import com.company.app.ordermanager.view.JsonViews;
import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderControllerImpl {
    private final OrderService orderService;

    @GetMapping
    @JsonView(JsonViews.ListView.class)
    public Page<Order> getOrdersList(@QuerydslPredicate(root = Order.class) Predicate predicate, Pageable pageable) {
        return orderService.findAll(predicate, pageable);
    }

    @GetMapping("/{id}")
    @JsonView(JsonViews.DetailView.class)
    public Order getOrderById(@PathVariable("id") UUID id) {
        return orderService.findById(id);
    }

    @PostMapping
    @JsonView(JsonViews.DetailView.class)
    public Order createOrder(@Valid @RequestBody CreateOrderDto order) {
        return orderService.createOrder(order);
    }

    @DeleteMapping("/{id}")
    public void deleteOrderById(@PathVariable("id") UUID id) {
        orderService.deleteById(id);
    }
}
