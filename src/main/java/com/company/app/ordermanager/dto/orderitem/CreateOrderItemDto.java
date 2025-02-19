package com.company.app.ordermanager.dto.orderitem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Represents the data transfer object (DTO) for creating an order item.
 * This class encapsulates the required details to specify a product
 * and quantity when adding it as an item to an order.
 */
@Data
public class CreateOrderItemDto {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
