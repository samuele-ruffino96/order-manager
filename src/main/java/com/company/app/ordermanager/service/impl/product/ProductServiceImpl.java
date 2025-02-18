package com.company.app.ordermanager.service.impl.product;

import com.company.app.ordermanager.dto.product.ProductDto;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.repository.api.product.ProductRepository;
import com.company.app.ordermanager.service.api.product.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private static final String STOCK_VALUE_KEY_PREFIX = "stock:";
    private static final Duration STOCK_VALUE_CACHE_EXPIRY = Duration.ofHours(1);

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public ProductDto getProduct(UUID productId) {
        Assert.notNull(productId, "Product ID must not be null");

        return productRepository.findById(productId)
                .map(product -> objectMapper.convertValue(product, ProductDto.class))
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

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
     * Utility method that constructs a unique stock key for a given product.
     * The generated key combines a predefined prefix with the string representation
     * of the provided {@link UUID} of the product.
     *
     * @param productId A {@link UUID} representing the unique identifier of the product.
     * @return A {@link String} representing the unique stock key, created by appending the {@code productId} to a predefined prefix.
     */
    private String getStockValueKey(UUID productId) {
        return STOCK_VALUE_KEY_PREFIX + productId.toString();
    }
}
