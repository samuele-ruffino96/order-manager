package com.company.app.ordermanager.exception.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Indicates a version mismatch for a specified product during an operation.
 * <p>
 * This exception is thrown when the version of the product in the request does not match the
 * version in the database, which is typically used for optimistic locking or consistency checks.
 * <p>
 * This exception results in an HTTP 400 Bad Request response when used in a Spring Web
 * environment, as indicated by the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ProductVersionMismatchException extends RuntimeException {
    public ProductVersionMismatchException(UUID productId, Long expectedVersion, Long actualVersion) {
        super(String.format("Product version mismatch for product %s. Expected: %d, Found: %d",
                productId, expectedVersion, actualVersion));
    }
}
