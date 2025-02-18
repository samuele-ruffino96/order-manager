package com.company.app.ordermanager.controller.order.impl;

import com.company.app.ordermanager.controller.order.api.OrderItemController;
import com.company.app.ordermanager.service.api.orderitem.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderItemControllerImpl implements OrderItemController {
    private final OrderItemService orderItemService;

    @DeleteMapping
    public void deleteOrderItemsByIds(@RequestParam("ids") Set<UUID> ids) {
        orderItemService.cancelOrderItems(ids);
    }
}
