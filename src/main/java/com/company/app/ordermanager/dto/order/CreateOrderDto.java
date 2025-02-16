package com.company.app.ordermanager.dto.order;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class CreateOrderDto {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String description;

    @NotEmpty(message = "Order must contain at least one item")
    private Set<@Valid CreateOrderItemDto> items;
}
