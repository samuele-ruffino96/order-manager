package com.company.app.ordermanager.service.impl.orderitem;

import com.company.app.ordermanager.dto.orderitem.OrderItemDto;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
import com.company.app.ordermanager.exception.orderitem.OrderItemNotFoundException;
import com.company.app.ordermanager.redis.stream.service.api.stock.StockStreamService;
import com.company.app.ordermanager.repository.api.orderitem.OrderItemRepository;
import com.company.app.ordermanager.service.api.orderitem.OrderItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final StockStreamService stockStreamService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Set<OrderItemDto> cancelOrderItems(Set<UUID> orderItemIds) {
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
        stockStreamService.sendStockCancellationMessage(savedOrderItems);

        return savedOrderItems.stream().map(orderItem -> objectMapper.convertValue(orderItem, OrderItemDto.class)).collect(Collectors.toSet());
    }

    @Override
    public void updateOrderItemStatus(UUID orderItemId, OrderItemStatus status, long version) {
        int updatedRows = orderItemRepository.updateStatus(orderItemId, status, version);

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
