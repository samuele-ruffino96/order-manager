package com.company.app.ordermanager.search.document;

import com.company.app.ordermanager.entity.order.Order;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an Order document in the Meilisearch index.
 * This is a simplified version of the Order entity
 * optimized for search operations.
 */
@Data
@Builder
public class OrderDocument {
    private UUID id;

    private String customerName;

    private String description;

    private Instant createdAt;

    private int totalItems;

    /**
     * Converts an {@link Order} entity into an {@link OrderDocument} for search index purposes.
     *
     * @param order the {@link Order} entity to be converted
     * @return a new instance of {@link OrderDocument} representing the given order
     */
    public static OrderDocument fromEntity(Order order) {
        return OrderDocument.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .description(order.getDescription())
                .createdAt(order.getCreatedAt())
                .totalItems(order.getOrderItems().size())
                .build();
    }
}
