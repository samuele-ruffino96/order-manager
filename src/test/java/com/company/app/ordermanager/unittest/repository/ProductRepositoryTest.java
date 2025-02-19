package com.company.app.ordermanager.unittest.repository;

import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.repository.api.product.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Save test entities
        testProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stockLevel(10)
                .build();

        entityManager.persist(testProduct);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void updateStockLevel_WhenProductExists_ShouldUpdateStock() {
        // Given
        int newStockLevel = 20;
        long initialVersion = testProduct.getVersion();

        // When
        int updatedRows = productRepository.updateStockLevel(testProduct.getId(), newStockLevel);

        // Then
        assertThat(updatedRows).isEqualTo(1);

        Product updated = entityManager.find(Product.class, testProduct.getId());
        assertThat(updated.getStockLevel()).isEqualTo(newStockLevel);
        assertThat(updated.getVersion()).isEqualTo(initialVersion + 1);
    }

    @Test
    void updateStockLevel_WhenProductDoesNotExist_ShouldReturnZero() {
        // When
        int updatedRows = productRepository.updateStockLevel(UUID.randomUUID(), 20);

        // Then
        assertThat(updatedRows).isZero();
    }
}
