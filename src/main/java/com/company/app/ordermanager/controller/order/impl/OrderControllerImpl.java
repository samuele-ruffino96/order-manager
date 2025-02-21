package com.company.app.ordermanager.controller.order.impl;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.view.JsonViews;
import com.company.app.ordermanager.search.dto.OrderSearchRequest;
import com.company.app.ordermanager.search.dto.OrderSearchResult;
import com.company.app.ordermanager.search.service.api.OrderSearchService;
import com.company.app.ordermanager.service.api.order.OrderService;
import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderControllerImpl {
    private final OrderService orderService;
    private final OrderSearchService orderSearchService;

    @GetMapping
    @JsonView(JsonViews.ListView.class)
    @Operation(
            summary = "Get orders list",
            description = "Retrieves a paginated list of orders with optional filtering using QueryDSL predicates"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved orders",
            useReturnTypeSchema = true
    )
    public Page<Order> getOrdersList(
            @Parameter(description = "Filter criteria using QueryDSL") @QuerydslPredicate(root = Order.class) Predicate predicate,
            @Parameter(description = "Pagination parameters") Pageable pageable) {
        return orderService.findAll(predicate, pageable);
    }

    @GetMapping("/search")
    @JsonView(JsonViews.ListView.class)
    @Operation(
            summary = "Search orders",
            description = "Search orders using full-text search and filters"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved orders",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "500", description = "Unexpected error during full-text search")
    public Page<OrderSearchResult> searchOrders(
            OrderSearchRequest searchRequest,
            Pageable pageable) {
        return orderSearchService.searchOrders(searchRequest, pageable);
    }

    @GetMapping("/{id}")
    @JsonView(JsonViews.DetailView.class)
    @Operation(
            summary = "Get order by ID",
            description = "Retrieves detailed information about a specific order"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Order.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "Order not found")
    public Order getOrderById(@Parameter(description = "UUID of the order to retrieve") @PathVariable("id") UUID id) {
        return orderService.findById(id);
    }

    @PostMapping
    @JsonView(JsonViews.DetailView.class)
    @Operation(
            summary = "Create a new order",
            description = "Creates a new order with the provided details and initiates stock reservation"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order created successfully",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "400", description = "Invalid order data")
    @ApiResponse(responseCode = "404", description = "Referenced product not found")
    public Order createOrder(@Parameter(description = "Order creation details", required = true) @Valid @RequestBody CreateOrderDto order) {
        return orderService.createOrder(order);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancel order by ID",
            description = "Cancels an order and cancels all associated order items, releasing reserved stock"
    )
    @ApiResponse(responseCode = "200", description = "Order successfully deleted")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public void deleteOrderById(@Parameter(description = "UUID of the order to delete") @PathVariable("id") UUID id) {
        orderService.deleteById(id);
    }
}
