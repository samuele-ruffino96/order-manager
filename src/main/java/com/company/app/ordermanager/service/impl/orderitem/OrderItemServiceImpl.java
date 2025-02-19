package com.company.app.ordermanager.service.impl.orderitem;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.orderitem.OrderItemNotFoundException;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.product.ProductVersionMismatchException;
import com.company.app.ordermanager.messaging.service.api.stock.StockMessageProducerService;
import com.company.app.ordermanager.repository.api.orderitem.OrderItemRepository;
import com.company.app.ordermanager.service.api.orderitem.OrderItemService;
import com.company.app.ordermanager.service.api.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final ProductService productService;
    private final OrderItemRepository orderItemRepository;
    private final StockMessageProducerService stockMessageProducerService;

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
    @Override
    @Transactional
    public Set<OrderItem> createOrderItems(Order order, Set<CreateOrderItemDto> orderItemDtos) {
        Assert.notNull(order, "Order must not be null");
        Assert.notNull(orderItemDtos, "Order item DTOs must not be null");

        // Fetch all products
        Set<UUID> productIds = orderItemDtos.stream().map(CreateOrderItemDto::getProductId).collect(Collectors.toSet());
        Map<UUID, Product> productsMap = productService.findAllById(productIds).stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        // Validate that all products exist
        if (productsMap.size() != productIds.size()) {
            Set<UUID> missingProducts = productIds.stream()
                    .filter(id -> !productsMap.containsKey(id))
                    .collect(Collectors.toSet());
            throw new ProductNotFoundException(missingProducts);
        }

        // Create order items
        Set<OrderItem> orderItems = orderItemDtos.stream()
                .map(itemDto -> {
                    Product product = productsMap.get(itemDto.getProductId());

                    // Validate product version to prevent stale data modifications
                    if (!Objects.equals(itemDto.getProductVersion(), (product.getVersion()))) {
                        throw new ProductVersionMismatchException(product.getId(), itemDto.getProductVersion(), product.getVersion());
                    }

                    return OrderItem.builder()
                            .order(order)
                            .product(product)
                            .quantity(itemDto.getQuantity())
                            .purchasePrice(product.getPrice())
                            .status(OrderItemStatus.PROCESSING)
                            .build();
                })
                .collect(Collectors.toSet());

        return orderItemRepository.saveAll(orderItems).stream().collect(Collectors.toSet());
    }

    /**
     * Cancels the specified order items by updating their status and reason, saving the changes,
     * and sending a stock cancellation message.
     *
     * @param orderItemIds a set of unique identifiers for the order items to be canceled
     * @return A set of order items after being updated with the cancellation status and reason
     * @throws IllegalArgumentException   if the orderItemIds parameter is null
     * @throws OrderItemNotFoundException if any of the specified order item IDs are not found
     */
    @Override
    @Transactional
    public Set<OrderItem> cancelOrderItems(Set<UUID> orderItemIds) {
        Assert.notNull(orderItemIds, "Order item IDs must not be null");

        // Fetch order items
        Set<OrderItem> orderItems = orderItemRepository.findAllById(orderItemIds).stream()
                .filter(i -> orderItemIds.contains(i.getId()))
                .map(orderItem -> {
                    orderItem.setStatus(OrderItemStatus.CANCELLING);
                    orderItem.setReason(OrderItemStatusReason.USER_CANCELLED);
                    return orderItem;
                })
                .collect(Collectors.toSet());

        // Validate that all order items exists
        if (orderItems.size() != orderItemIds.size()) {
            Set<UUID> foundedIds = orderItems.stream().map(OrderItem::getId).collect(Collectors.toSet());

            Set<UUID> missingOrderItems = orderItemIds.stream()
                    .filter(id -> !foundedIds.contains(id))
                    .collect(Collectors.toSet());

            throw new OrderItemNotFoundException(missingOrderItems);
        }

        // Update order items
        Set<OrderItem> savedOrderItems = orderItemRepository.saveAll(orderItems).stream().collect(Collectors.toSet());

        // Send stock reservation request to queue
        stockMessageProducerService.sendStockCancellationMessage(savedOrderItems);

        return savedOrderItems;
    }

    /**
     * Updates the status of an order item in the repository to the specified value.
     *
     * @param orderItemId      the unique identifier of the order item to update
     * @param status           the new status to be applied to the order item
     * @param orderItemVersion the current version of the order item to ensure versioning consistency
     * @throws IllegalArgumentException if {@code orderItemId} or {@code status} is null
     */
    @Override
    public void updateOrderItemStatus(UUID orderItemId, OrderItemStatus status, long orderItemVersion) {
        Assert.notNull(orderItemId, "Order item ID must not be null");
        Assert.notNull(status, "Order item status must not be null");

        int updatedRows = orderItemRepository.updateStatus(orderItemId, status, orderItemVersion);

        if (updatedRows == 0) {
            log.warn("Order item with ID {} not found. Unable to update status.", orderItemId);
        }
    }

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
    @Override
    public void updateOrderItemStatusAndReason(UUID orderItemId, OrderItemStatus status, long version, OrderItemStatusReason reason) {
        int updatedRows = orderItemRepository.updateStatusAndReason(orderItemId, status, version, reason);

        if (updatedRows == 0) {
            log.warn("Order item with ID {} not found. Unable to update status and reason.", orderItemId);
        }
    }
}
