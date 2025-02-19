package com.company.app.ordermanager.repository.api.product;

import com.company.app.ordermanager.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    /**
     * Updates the stock level of a product with the specified ID in the database.
     *
     * @param productId  the unique identifier of the product whose stock level is to be updated
     * @param stockLevel the new stock level value to set for the specified product
     * @return the number of rows affected by the update operation
     * @throws IllegalArgumentException if {@code productId} or {@code stockLevel} are invalid
     */
    @Modifying
    @Query("UPDATE Product p SET p.stockLevel = :stockLevel, p.version = p.version + 1 " +
            "WHERE p.id = :productId")
    int updateStockLevel(@Param("productId") UUID productId, @Param("stockLevel") int stockLevel);
}
