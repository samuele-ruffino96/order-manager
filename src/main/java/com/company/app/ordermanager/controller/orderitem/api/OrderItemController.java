package com.company.app.ordermanager.controller.orderitem.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public interface OrderItemController {
    void deleteOrderItemsByIds(@Valid @NotEmpty Set<UUID> ids);

    void deleteOrderItemsById(UUID id);
}
