package com.company.app.ordermanager.exception.orderitem;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;
import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderItemsNotFoundException extends RuntimeException {
    public OrderItemsNotFoundException(UUID orderId, Set<UUID> missingOrderItemIds) {
        super(String.format("Order items not found for order %s: %s", orderId, missingOrderItemIds));
    }
}
