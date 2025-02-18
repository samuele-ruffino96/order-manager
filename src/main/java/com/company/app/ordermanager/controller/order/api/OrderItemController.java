package com.company.app.ordermanager.controller.order.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;
import java.util.UUID;

public interface OrderItemController {
    void deleteOrderItemsByIds(@RequestParam("ids") Set<UUID> ids);

    void deleteOrderItemsById(@PathVariable("id") UUID id);
}
