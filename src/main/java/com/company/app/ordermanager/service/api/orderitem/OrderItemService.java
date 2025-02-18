package com.company.app.ordermanager.service.api.orderitem;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
import com.company.app.ordermanager.exception.orderitem.OrderItemNotFoundException;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.product.ProductVersionMismatchException;

import java.util.Set;
import java.util.UUID;

public interface OrderItemService {
    /**
     * Creates a set of {@link OrderItem} entities based on the provided {@link Order} and {@link CreateOrderItemDto}.
     *
     * @param order         the {@link Order} to associate with the created {@link OrderItem}s.
     * @param orderItemDtos the set of {@link CreateOrderItemDto} containing details for each {@link OrderItem}.
     * @return A set of newly created and persisted {@link OrderItem} entities.
     * @throws IllegalArgumentException        if either the order or orderItemDtos is null.
     * @throws ProductNotFoundException        if any referenced products in orderItemDtos are not found.
     * @throws ProductVersionMismatchException if the product versions in orderItemDtos do not match the current product versions.
     */
    Set<OrderItem> createOrderItems(Order order, Set<CreateOrderItemDto> orderItemDtos);

    /**
     * Cancels the specified order items by updating their status and reason, saving the changes,
     * and sending a stock cancellation message.
     *
     * @param orderItemIds a set of unique identifiers for the order items to be canceled
     * @return A set of order items after being updated with the cancellation status and reason
     * @throws IllegalArgumentException   if the orderItemIds parameter is null
     * @throws OrderItemNotFoundException if any of the specified order item IDs are not found
     */
    Set<OrderItem> cancelOrderItems(Set<UUID> orderItemIds);

    /**
     * Updates the status of an order item in the repository to the specified value.
     *
     * @param orderItemId      the unique identifier of the order item to update
     * @param status           the new status to be applied to the order item
     * @param orderItemVersion the current version of the order item to ensure versioning consistency
     * @throws IllegalArgumentException if {@code orderItemId} or {@code status} is null
     */
    void updateOrderItemStatus(UUID orderItemId, OrderItemStatus status, long orderItemVersion);

    /**
     * Updates the status and reason for a specific order item identified by its ID.
     *
     * @param orderItemId the unique identifier of the order item to be updated
     * @param status      the new status to set for the order item
     * @param version     the version of the order item, used to ensure data consistency
     * @param reason      the reason for the status change of the order item
     * @throws IllegalArgumentException   if any parameter is invalid
     * @throws OrderItemNotFoundException if the order item with the specified ID is not found
     */
    void updateOrderItemStatusAndReason(UUID orderItemId, OrderItemStatus status, long version, OrderItemStatusReason reason);

}
