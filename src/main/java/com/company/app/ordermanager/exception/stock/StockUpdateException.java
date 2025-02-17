package com.company.app.ordermanager.exception.stock;

public class StockUpdateException extends RuntimeException {
    public StockUpdateException(String message) {
        super(message);
    }

    public StockUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
