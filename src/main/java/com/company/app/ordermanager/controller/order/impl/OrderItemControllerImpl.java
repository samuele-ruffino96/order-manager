package com.company.app.ordermanager.controller.order.impl;

import com.company.app.ordermanager.controller.order.api.OrderItemController;
import com.company.app.ordermanager.service.api.orderitem.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{id}")
    public void deleteOrderItemsById(@PathVariable("id") UUID id) {
        orderItemService.cancelOrderItems(Set.of(id));
    }
}
