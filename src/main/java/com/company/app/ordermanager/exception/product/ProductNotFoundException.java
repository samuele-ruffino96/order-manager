package com.company.app.ordermanager.exception.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;
import java.util.UUID;

/**
 * Exception thrown when a product or products cannot be found.
 * <p>
 * This exception is used to indicate that the specified product(s) do not exist
 * in the system.
 * <p>
 * It returns a HTTP 404 Not Found status code when used in a
 * Spring Web environment, as specified by the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super(String.format("Product with ID %s not found", productId));
    }

    public ProductNotFoundException(Set<UUID> productIds) {
        super(String.format("Products not found: %s", productIds));
    }
}
