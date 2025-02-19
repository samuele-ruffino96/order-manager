package com.company.app.ordermanager.service.impl.product;

import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.repository.api.product.ProductRepository;
import com.company.app.ordermanager.service.api.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private static final String STOCK_VALUE_KEY_PREFIX = "stock:";
    private static final Duration STOCK_VALUE_CACHE_EXPIRY = Duration.ofHours(1);

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * Retrieves a {@link Product} entity by its unique identifier.
     * If the product is not found, an exception is thrown.
     *
     * @param productId A {@link UUID} representing the unique identifier of the product.
     * @return The {@link Product} associated with the given identifier.
     * @throws IllegalArgumentException If the provided {@code productId} is null.
     * @throws ProductNotFoundException If no product is found with the given identifier.
     */
    @Override
    public Product findById(UUID productId) {
        Assert.notNull(productId, "Product ID must not be null");

        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    /**
     * Retrieves all {@link Product} entities matching the provided set of unique identifiers.
     * Products are fetched as a set to maintain uniqueness.
     *
     * @param productIds A set of {@link UUID} identifying the products to retrieve.
     * @return A set of {@link Product} entities corresponding to the provided identifiers.
     * @throws IllegalArgumentException If the provided {@code productIds} set is null.
     */
    @Override
    public Set<Product> findAllById(Set<UUID> productIds) {
        return productRepository.findAllById(productIds).stream().collect(Collectors.toSet());
    }

    /**
     * Retrieves the stock level of a {@link Product} identified by its unique ID.
     * The method checks for the stock data in a Redis cache; if not found, it fetches
     * the stock level from the database, updates the cache, and returns the value.
     *
     * @param productId A {@link UUID} representing the unique identifier of the product.
     * @return An {@code int} representing the stock level of the product.
     * @throws IllegalArgumentException If the provided {@code productId} is null.
     * @throws ProductNotFoundException If no product is found with the given identifier.
     */
    @Override
    public int getProductStockLevel(UUID productId) {
        Assert.notNull(productId, "Product ID must not be null");

        String productStockKey = getStockValueKey(productId);

        String currentStock = redisTemplate.opsForValue().get(productStockKey);

        // If stock not in cache, fetch from database
        if (currentStock == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            currentStock = String.valueOf(product.getStockLevel());

            // Cache the stock with 1 hour expiry
            redisTemplate.opsForValue().set(productStockKey, currentStock, STOCK_VALUE_CACHE_EXPIRY);
        }

        return Integer.parseInt(currentStock);
    }

    /**
     * Updates the stock level for a specified product.
     *
     * @param productId     the unique identifier of the product whose stock level needs to be updated
     * @param newStockLevel the new stock level to set for the product
     * @throws IllegalArgumentException if productId is null or newStockLevel is negative
     */
    @Override
    public void updateProductStockLevel(UUID productId, int newStockLevel) {
        Assert.notNull(productId, "Product ID must not be null");
        Assert.isTrue(newStockLevel >= 0, "Stock level must be greater than or equal to 0");

        String productStockKey = getStockValueKey(productId);

        // Update cache
        redisTemplate.opsForValue().set(productStockKey, String.valueOf(newStockLevel));

        // Update product entity
        int updatedRows = productRepository.updateStockLevel(productId, newStockLevel);

        if (updatedRows == 0) {
            log.warn("Product with ID {} not found. Unable to update stock level.", productId);
        }
    }

    /**
     * Generates a unique key for storing or retrieving stock values in Redis for a specific product.
     *
     * @param productId a {@link UUID} representing the unique identifier of the product.
     * @return a {@link String} representing the Redis key for the stock value of the product.
     */
    private String getStockValueKey(UUID productId) {
        return STOCK_VALUE_KEY_PREFIX + productId.toString();
    }
}
