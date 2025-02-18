package com.company.app.ordermanager.exception.orderitem;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;
import java.util.UUID;

/**
 * Exception thrown when an order item or multiple order items cannot be found.
 * <p>
 * This exception is typically used in scenarios where a requested order item or a set of order
 * items are not present in the system. It provides specific details about the missing entities
 * using their respective IDs.
 * <p>
 * It returns a HTTP 404 Not Found status code when used in a
 * Spring Web environment, as specified by the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderItemNotFoundException extends RuntimeException {
    public OrderItemNotFoundException(UUID orderItemId) {
        super(String.format("Order item with ID %s not found", orderItemId));
    }

    public OrderItemNotFoundException(Set<UUID> orderItemIds) {
        super(String.format("Order items not found: %s", orderItemIds));
    }
}
