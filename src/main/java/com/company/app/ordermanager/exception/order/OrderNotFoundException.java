package com.company.app.ordermanager.exception.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exception thrown when an {@link Order} entity with a specified identifier is not found.
 * This typically occurs when attempting to retrieve or manipulate an order that does not
 * exist in the system.
 * <p>
 * It returns a HTTP 404 Not Found status code when used in a
 * Spring Web environment, as specified by the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super(String.format("Order with ID %s not found", orderId));
    }
}
