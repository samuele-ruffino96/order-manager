package com.company.app.ordermanager.dto.order;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

/**
 * Represents the data transfer object (DTO) for creating an order.
 * This class encapsulates the necessary details for creating a customer order,
 * including customer information, order description, and a collection of order items.
 */
@Data
public class CreateOrderDto {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String description;

    @NotEmpty(message = "Order must contain at least one item")
    private Set<@Valid CreateOrderItemDto> items;
}
