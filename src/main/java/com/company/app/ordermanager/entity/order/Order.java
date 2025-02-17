package com.company.app.ordermanager.entity.order;

import com.company.app.ordermanager.entity.common.Auditable;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an order within the system.
 * <p>
 * The {@code Order} class is responsible for encapsulating all details about a customer's order,
 * including the customer's name, order status, and associated {@link OrderItem}s.
 * It extends the {@link Auditable} class to provide audit details such as creation and modification timestamps.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(exclude = "orderItems")
@SuperBuilder
@Entity
@Table(name = "orders")
public class Order extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> orderItems = new HashSet<>();

    @Transient
    public OrderStatus getStatus() {
        if (allItemsHaveStatus(OrderItemStatus.CONFIRMED)) {
            return OrderStatus.CONFIRMED;
        }
        if (allItemsHaveStatus(OrderItemStatus.CANCELLED)) {
            return OrderStatus.CANCELLED;
        }
        if (hasItemsWithStatus(OrderItemStatus.PROCESSING, OrderItemStatus.CANCELLING)) {
            return OrderStatus.PROCESSING;
        }
        if (hasItemsWithStatus(OrderItemStatus.PROCESSING_FAILED, OrderItemStatus.CANCELLED)) {
            return OrderStatus.PARTIALLY_CONFIRMED;
        }
        return OrderStatus.UNKNOWN;
    }

    private boolean hasItemsWithStatus(OrderItemStatus... statuses) {
        return orderItems.stream()
                .map(OrderItem::getStatus)
                .anyMatch(status -> Arrays.asList(statuses).contains(status));
    }

    private boolean hasAnyItemWithStatus(OrderItemStatus status) {
        return orderItems.stream()
                .map(OrderItem::getStatus)
                .anyMatch(status::equals);
    }

    private boolean allItemsHaveStatus(OrderItemStatus status) {
        return orderItems.stream()
                .map(OrderItem::getStatus)
                .allMatch(status::equals);
    }
}
