package com.company.app.ordermanager.unittest.service;

import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.repository.api.product.ProductRepository;
import com.company.app.ordermanager.service.impl.product.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    private static final String STOCK_VALUE_KEY_PREFIX = "stock:";
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create entities
        testProduct = Product.builder()
                .id(PRODUCT_ID)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .stockLevel(10)
                .version(1L)
                .build();
    }

    @Test
    void findById_WhenProductExists_ShouldReturnProduct() {
        // Given
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(testProduct));

        // When
        Product result = productService.findById(PRODUCT_ID);

        // Then
        assertThat(result).isEqualTo(testProduct);
    }

    @Test
    void findById_WhenProductDoesNotExist_ShouldThrowException() {
        // Given
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ProductNotFoundException.class, () ->
                productService.findById(PRODUCT_ID)
        );
    }

    @Test
    void findAllById_ShouldReturnSetOfProducts() {
        // Given
        Set<UUID> productIds = Set.of(PRODUCT_ID);
        when(productRepository.findAllById(productIds)).thenReturn(List.of(testProduct));

        // When
        Set<Product> results = productService.findAllById(productIds);

        // Then
        assertThat(results)
                .hasSize(1)
                .contains(testProduct);
    }

    @Test
    void getProductStockLevel_WhenCacheHit_ShouldReturnCachedValue() {
        // Given
        String stockKey = STOCK_VALUE_KEY_PREFIX + PRODUCT_ID;
        when(valueOperations.get(stockKey)).thenReturn("5");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        int result = productService.getProductStockLevel(PRODUCT_ID);

        // Then
        assertThat(result).isEqualTo(5);
        verify(productRepository, never()).findById(any());
    }

    @Test
    void getProductStockLevel_WhenCacheMiss_ShouldFetchFromDB() {
        // Given
        String stockKey = STOCK_VALUE_KEY_PREFIX + PRODUCT_ID;
        when(valueOperations.get(stockKey)).thenReturn(null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(testProduct));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        int result = productService.getProductStockLevel(PRODUCT_ID);

        // Then
        assertThat(result).isEqualTo(testProduct.getStockLevel());
        verify(valueOperations).set(eq(stockKey), eq("10"), any(Duration.class));
    }

    @Test
    void updateProductStockLevel_ShouldUpdateCacheAndDB() {
        // Given
        String stockKey = STOCK_VALUE_KEY_PREFIX + PRODUCT_ID;
        int newStockLevel = 20;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        productService.updateProductStockLevel(PRODUCT_ID, newStockLevel);

        // Then
        verify(valueOperations).set(stockKey, String.valueOf(newStockLevel));
        verify(productRepository).updateStockLevel(PRODUCT_ID, newStockLevel);
    }

    @Test
    void updateProductStockLevel_WhenNegativeStock_ShouldThrowException() {
        // Given
        int negativeStock = -1;

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
                productService.updateProductStockLevel(PRODUCT_ID, negativeStock)
        );

        verify(valueOperations, never()).set(any(), any());
        verify(productRepository, never()).updateStockLevel(any(), anyInt());
    }
}
