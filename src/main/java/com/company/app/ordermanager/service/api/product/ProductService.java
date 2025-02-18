package com.company.app.ordermanager.service.api.product;

import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;

import java.util.Set;
import java.util.UUID;

public interface ProductService {
    /**
     * Retrieves a {@link Product} entity by its unique identifier.
     * If the product is not found, an exception is thrown.
     *
     * @param productId A {@link UUID} representing the unique identifier of the product.
     *
     * @return The {@link Product} associated with the given identifier.
     *
     * @throws IllegalArgumentException If the provided {@code productId} is null.
     * @throws ProductNotFoundException If no product is found with the given identifier.
     */
    Product findById(UUID productId);

    /**
     * Retrieves all {@link Product} entities matching the provided set of unique identifiers.
     * Products are fetched as a set to maintain uniqueness.
     *
     * @param productIds A set of {@link UUID} identifying the products to retrieve.
     * @return A set of {@link Product} entities corresponding to the provided identifiers.
     * @throws IllegalArgumentException If the provided {@code productIds} set is null.
     */
    Set<Product> findAllById(Set<UUID> productIds);

    /**
     * Retrieves the stock level of a {@link Product} identified by its unique ID.
     * The method checks for the stock data in a Redis cache; if not found, it fetches
     * the stock level from the database, updates the cache, and returns the value.
     *
     * @param productId A {@link UUID} representing the unique identifier of the product.
     *
     * @return An {@code int} representing the stock level of the product.
     *
     * @throws IllegalArgumentException If the provided {@code productId} is null.
     * @throws ProductNotFoundException If no product is found with the given identifier.
     */
    int getProductStockLevel(UUID productId);

    /**
     * Updates the stock level for a specified product.
     *
     * @param productId     the unique identifier of the product whose stock level needs to be updated
     * @param newStockLevel the new stock level to set for the product
     *
     * @throws IllegalArgumentException if productId is null or newStockLevel is negative
     */
    void updateProductStockLevel(UUID productId, int newStockLevel);
}
