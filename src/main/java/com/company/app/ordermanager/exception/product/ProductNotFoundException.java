package com.company.app.ordermanager.exception.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;
import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super(String.format("Product with ID %s not found", productId));
    }

    public ProductNotFoundException(Set<UUID> productIds) {
        super(String.format("Products not found: %s", productIds));
    }
}
