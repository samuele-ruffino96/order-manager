package com.company.app.ordermanager.exception.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;


@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ProductVersionMismatchException extends RuntimeException {
    public ProductVersionMismatchException(UUID productId, Long expectedVersion, Long actualVersion) {
        super(String.format("Product version mismatch for product %s. Expected: %d, Found: %d",
                productId, expectedVersion, actualVersion));
    }
}
