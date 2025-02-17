package com.company.app.ordermanager.redis.stream.service.api.stock;

public interface StockStreamProcessor {
    /**
     * This method processes stock updates by consuming messages from a Redis stream
     * and performing necessary operations.
     * It ensures message acknowledgment after successful processing and trims the stream
     * to avoid unbounded growth.
     * <p>
     * It handles parsing errors and ensures failed updates can be retried without acknowledgment.
     * </p>
     */
    void processStockUpdateMessages();
}
