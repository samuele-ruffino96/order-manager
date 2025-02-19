package com.company.app.ordermanager.messaging.service.api.stock;

import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.stock.StockLockException;
import com.company.app.ordermanager.messaging.dto.StockUpdateMessage;

public interface StockMessageConsumerService {
    void processStockUpdateMessage(StockUpdateMessage message);
}
