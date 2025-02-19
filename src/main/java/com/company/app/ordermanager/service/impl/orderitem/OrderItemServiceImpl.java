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

    @Override
    public void updateOrderItemStatus(UUID orderItemId, OrderItemStatus status, long orderItemVersion) {
        Assert.notNull(orderItemId, "Order item ID must not be null");
        Assert.notNull(status, "Order item status must not be null");

        int updatedRows = orderItemRepository.updateStatus(orderItemId, status, orderItemVersion);

        if (updatedRows == 0) {
            log.warn("Order item with ID {} not found. Unable to update status.", orderItemId);
        }
    }

    @Override
    public void updateOrderItemStatusAndReason(UUID orderItemId, OrderItemStatus status, long version, OrderItemStatusReason reason) {
        int updatedRows = orderItemRepository.updateStatusAndReason(orderItemId, status, version, reason);

        if (updatedRows == 0) {
            log.warn("Order item with ID {} not found. Unable to update status and reason.", orderItemId);
        }
    }
}
