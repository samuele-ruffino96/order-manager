package com.company.app.ordermanager.controller.orderitem.impl;

import com.company.app.ordermanager.controller.orderitem.api.OrderItemController;
import com.company.app.ordermanager.service.api.orderitem.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order-items")
@Tag(name = "Order Items", description = "Order items management endpoints")
public class OrderItemControllerImpl implements OrderItemController {
    private final OrderItemService orderItemService;

    @DeleteMapping
    @Operation(
            summary = "Cancel multiple order items",
            description = "Cancels the specified order items and releases their reserved stock."
    )
    @ApiResponse(responseCode = "200", description = "Order items successfully cancelled and stock released")
    @ApiResponse(responseCode = "400", description = "Invalid request - empty item list or invalid item IDs")
    @ApiResponse(responseCode = "404", description = "One or more order items not found")
    public void deleteOrderItemsByIds(
            @Parameter(
                    description = "Set of order item UUIDs to cancel. Must not be empty.",
                    required = true,
                    example = "['123e4567-e89b-12d3-a456-426614174000', '123e4567-e89b-12d3-a456-426614174001']"
            )
            @RequestParam("ids") @Valid @NotEmpty Set<UUID> ids) {
        orderItemService.cancelOrderItems(ids);
    }


    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancel an order item by ID",
            description = "Cancels an order item and releases its reserved stock"
    )
    @ApiResponse(responseCode = "200", description = "Order item successfully cancelled and stock released")
    @ApiResponse(responseCode = "404", description = "Order item not found")
    @ApiResponse(responseCode = "400", description = "Invalid request - invalid item ID")
    public void deleteOrderItemsById(@PathVariable("id") UUID id) {
        orderItemService.cancelOrderItems(Set.of(id));
    }
}
