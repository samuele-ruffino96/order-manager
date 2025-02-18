package com.company.app.ordermanager.unittest;

import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.repository.api.orderitem.OrderItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderItemRepositoryTest {
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private EntityManager entityManager;

    private OrderItem testOrderItem;
    private Order testOrder;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create test product
        testProduct = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .stockLevel(10)
                .build();
        entityManager.persist(testProduct);

        // Create test order
        testOrder = Order.builder()
                .customerName("Test Customer")
                .description("Test Order")
                .build();
        entityManager.persist(testOrder);

        // Create test order item
        testOrderItem = OrderItem.builder()
                .order(testOrder)
                .product(testProduct)
                .quantity(2)
                .purchasePrice(new BigDecimal("99.99"))
                .status(OrderItemStatus.PROCESSING)
                .version(1)
                .build();
        entityManager.persist(testOrderItem);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void updateStatus_WhenVersionMatches_ShouldUpdateStatus() {
        // Given
        OrderItemStatus newStatus = OrderItemStatus.CONFIRMED;
        long currentVersion = testOrderItem.getVersion();

        // When
        int updatedRows = orderItemRepository.updateStatus(
                testOrderItem.getId(),
                newStatus,
                currentVersion
        );

        // Then
        assertThat(updatedRows).isEqualTo(1);

        // Verify the update in database
        OrderItem updatedItem = entityManager.find(OrderItem.class, testOrderItem.getId());
        assertThat(updatedItem.getStatus()).isEqualTo(newStatus);
        assertThat(updatedItem.getVersion()).isEqualTo(currentVersion + 1);
    }

    @Test
    void updateStatus_WhenVersionDoesNotMatch_ShouldNotUpdateStatus() {
        // Given
        OrderItemStatus newStatus = OrderItemStatus.CONFIRMED;
        long wrongVersion = testOrderItem.getVersion() + 1;

        // When
        int updatedRows = orderItemRepository.updateStatus(
                testOrderItem.getId(),
                newStatus,
                wrongVersion
        );

        // Then
        assertThat(updatedRows).isZero();

        // Verify no changes in database
        OrderItem unchangedItem = entityManager.find(OrderItem.class, testOrderItem.getId());
        assertThat(unchangedItem.getStatus()).isEqualTo(OrderItemStatus.PROCESSING);
        assertThat(unchangedItem.getVersion()).isEqualTo(testOrderItem.getVersion());
    }

    @Test
    void updateStatusAndReason_WhenVersionMatches_ShouldUpdateBothFields() {
        // Given
        OrderItemStatus newStatus = OrderItemStatus.CANCELLED;
        OrderItemStatusReason reason = OrderItemStatusReason.USER_CANCELLED;
        long currentVersion = testOrderItem.getVersion();

        // When
        int updatedRows = orderItemRepository.updateStatusAndReason(
                testOrderItem.getId(),
                newStatus,
                currentVersion,
                reason
        );

        // Then
        assertThat(updatedRows).isEqualTo(1);

        // Verify the update in database
        OrderItem updatedItem = entityManager.find(OrderItem.class, testOrderItem.getId());
        assertThat(updatedItem.getStatus()).isEqualTo(newStatus);
        assertThat(updatedItem.getReason()).isEqualTo(reason);
        assertThat(updatedItem.getVersion()).isEqualTo(currentVersion + 1);
    }

    @Test
    void updateStatusAndReason_WhenVersionDoesNotMatch_ShouldNotUpdate() {
        // Given
        OrderItemStatus newStatus = OrderItemStatus.CANCELLED;
        OrderItemStatusReason reason = OrderItemStatusReason.USER_CANCELLED;
        long wrongVersion = testOrderItem.getVersion() + 1;

        // When
        int updatedRows = orderItemRepository.updateStatusAndReason(
                testOrderItem.getId(),
                newStatus,
                wrongVersion,
                reason
        );

        // Then
        assertThat(updatedRows).isZero();

        // Verify no changes in database
        OrderItem unchangedItem = entityManager.find(OrderItem.class, testOrderItem.getId());
        assertThat(unchangedItem.getStatus()).isEqualTo(OrderItemStatus.PROCESSING);
        assertThat(unchangedItem.getReason()).isNull();
        assertThat(unchangedItem.getVersion()).isEqualTo(testOrderItem.getVersion());
    }

    @Test
    void findVersionById_WhenOrderItemExists_ShouldReturnVersion() {
        // When
        Optional<Long> version = orderItemRepository.findVersionById(testOrderItem.getId());

        // Then
        assertThat(version)
                .isPresent()
                .contains(testOrderItem.getVersion());
    }

    @Test
    void findVersionById_WhenOrderItemDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Long> version = orderItemRepository.findVersionById(java.util.UUID.randomUUID());

        // Then
        assertThat(version).isEmpty();
    }
}
