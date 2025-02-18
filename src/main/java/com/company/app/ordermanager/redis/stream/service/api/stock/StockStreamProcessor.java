package com.company.app.ordermanager.redis.stream.service.api.stock;

import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.stock.StockLockException;
import com.company.app.ordermanager.redis.stream.dto.StockUpdateMessage;

public interface StockStreamProcessor {
    /**
     * Processes a stock update based on the type of the received message.
     *
     * @param message the {@link StockUpdateMessage} containing details about the stock update.
     * @throws IllegalArgumentException if the {@code message} contains invalid or inconsistent data.
     * @throws ProductNotFoundException if no product is found with the product ID within the stock update message
     * @throws StockLockException if the method is interrupted while acquiring the product lock
     */
    void processStockUpdate(StockUpdateMessage message);
}
