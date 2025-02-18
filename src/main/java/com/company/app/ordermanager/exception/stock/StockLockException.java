package com.company.app.ordermanager.exception.stock;

/**
 * Represents an exception that is thrown when an error occurs related to stock lock operations.
 */
public class StockLockException extends RuntimeException {
    public StockLockException(String message) {
        super(message);
    }

    public StockLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
