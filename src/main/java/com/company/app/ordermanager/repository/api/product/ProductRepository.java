package com.company.app.ordermanager.repository.api.product;

import com.company.app.ordermanager.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Modifying
    @Query("UPDATE Product p SET p.stockLevel = :stockLevel WHERE p.id = :productId")
    int updateStockLevel(@Param("productId") UUID productId, @Param("stockLevel") int stockLevel);
}
