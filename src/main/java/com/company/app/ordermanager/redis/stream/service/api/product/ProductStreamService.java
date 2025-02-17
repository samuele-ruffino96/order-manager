package com.company.app.ordermanager.redis.stream.service.api.product;

import java.util.UUID;

public interface ProductStreamService {
    void sendProductStockLevelUpdateMessage(UUID productId, Integer newStockLevel);
}
