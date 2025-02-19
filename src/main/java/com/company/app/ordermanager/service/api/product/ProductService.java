package com.company.app.ordermanager.service.api.product;

import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;

import java.util.Set;
import java.util.UUID;

public interface ProductService {
    Product findById(UUID productId);

    Set<Product> findAllById(Set<UUID> productIds);

    int getProductStockLevel(UUID productId);

    void updateProductStockLevel(UUID productId, int newStockLevel);
}
