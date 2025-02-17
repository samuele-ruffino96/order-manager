package com.company.app.ordermanager.redis.stream.service.api.product;

import java.util.UUID;

public interface ProductStreamService {
    void requestProductStockLevelUpdate(UUID productId, Integer newStockLevel);
}
