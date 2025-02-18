package com.company.app.ordermanager.exception.orderitem;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;
import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderItemNotFoundException extends RuntimeException {
    public OrderItemNotFoundException(UUID orderItemId) {
        super(String.format("Order item with ID %s not found", orderItemId));
    }

    public OrderItemNotFoundException(Set<UUID> orderItemIds) {
        super(String.format("Order items not found: %s", orderItemIds));
    }
}
