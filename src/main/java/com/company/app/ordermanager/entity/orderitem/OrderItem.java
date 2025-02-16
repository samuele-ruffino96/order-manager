package com.company.app.ordermanager.entity.orderitem;

import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.order.OrderStatus;
import com.company.app.ordermanager.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an individual ordered item within an {@link Order}.
 * <p>
 * The {@code OrderItem} class associates a single {@link Product} with an {@link Order},
 * while also capturing the quantity of the product ordered and its price at the time of purchase.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"order", "product"})
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PROCESSING;
}
