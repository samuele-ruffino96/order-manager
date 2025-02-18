package com.company.app.ordermanager.exception.stock;

public class StockLockException extends RuntimeException {
    public StockLockException(String message) {
        super(message);
    }

    public StockLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
