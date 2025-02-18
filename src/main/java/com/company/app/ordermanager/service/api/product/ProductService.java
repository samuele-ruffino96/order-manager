package com.company.app.ordermanager.service.api.product;

import com.company.app.ordermanager.dto.product.ProductDto;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.repository.api.product.ProductRepository;

import java.util.UUID;

public interface ProductService {
    /**
     * This method queries the {@link ProductRepository} to find a product by its ID,
     * converts the product entity to a {@link ProductDto}, and returns the result.
     * If the product is not found, a {@link ProductNotFoundException} is thrown.
     *
     * @param productId A {@link UUID} representing the unique identifier of the product.
     *                  This value must not be {@code null}.
     * @return A {@link ProductDto} object containing the product's information,
     * including its ID, name, description, price, stock level, and version.
     * @throws ProductNotFoundException If no product is found with the provided {@code productId}.
     * @throws IllegalArgumentException If the {@code productId} is {@code null}.
     */
    ProductDto getProduct(UUID productId);

    /**
     * Retrieves the current stock level of a product identified by its unique {@link UUID}.
     * <p>
     * This method first tries to fetch the stock level from a cache. If the stock level
     * is not present in the cache, it retrieves the stock level from the database, caches
     * it with a predefined expiration time, and then returns the value.
     * </p>
     *
     * @param productId A {@link UUID} representing the unique identifier of the product.
     *                  It must not be {@code null}.
     * @return The current stock level of the product as an {@code int}.
     * @throws IllegalArgumentException If the {@code productId} is {@code null}.
     * @throws ProductNotFoundException If the product with the specified {@code productId}
     *                                  cannot be found in the database.
     */
    int getProductStockLevel(UUID productId);

    /**
     * Updates the stock level of a product identified by the given {@link UUID productId}.
     * This method updates the stock level in both the cache and the database.
     *
     * <p>The stock level must be a non-negative integer, and the product ID must not be {@code null}.
     * If the product does not exist in the database, the method will log a warning but will not
     * throw an exception.
     *
     * @param productId     the unique identifier of the product whose stock level is being updated.
     *                      This parameter must not be {@code null}.
     * @param newStockLevel the new stock level to be set for the product. This value must be greater
     *                      than or equal to {@literal 0}.
     * @throws IllegalArgumentException if {@code productId} is {@code null} or {@code newStockLevel} is negative.
     */
    void updateProductStockLevel(UUID productId, int newStockLevel);
}
