package com.company.app.ordermanager.service.impl.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.dto.order.OrderDto;
import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.order.OrderNotFoundException;
import com.company.app.ordermanager.exception.orderitem.OrderItemsNotFoundException;
import com.company.app.ordermanager.redis.stream.service.api.StockStreamService;
import com.company.app.ordermanager.repository.api.order.OrderRepository;
import com.company.app.ordermanager.repository.api.product.ProductRepository;
import com.company.app.ordermanager.service.api.order.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final StockStreamService stockStreamService;

    @Override
    public OrderDto createOrder(CreateOrderDto createOrderDto) {
        Assert.notNull(createOrderDto, "Create order DTO must not be null");

        // Fetch all products in a single query
        Set<UUID> productIds = createOrderDto.getItems().stream()
                .map(CreateOrderItemDto::getProductId)
                .collect(Collectors.toSet());
        Map<UUID, Product> productsMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Validate that all products exist
        if (productsMap.size() != productIds.size()) {
            Set<UUID> missingProducts = productIds.stream()
                    .filter(id -> !productsMap.containsKey(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Products not found: " + missingProducts);
        }

        // Create order
        Order order = Order.builder()
                .customerName(createOrderDto.getCustomerName())
                .description(createOrderDto.getDescription())
                .build();

        // Create order items
        Set<OrderItem> orderItems = createOrderDto.getItems().stream()
                .map(itemDto -> {
                    Product product = productsMap.get(itemDto.getProductId());

                    // Validate product version to prevent stale data modifications
                    if (!Objects.equals(itemDto.getProductVersion(), (product.getVersion()))) {
                        throw new IllegalStateException(
                                String.format("Product version mismatch for product %s. Expected: %d, Found: %d",
                                        product.getId(), itemDto.getProductVersion(), product.getVersion())
                        );
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
        order.setOrderItems(orderItems);

        // Save the order and its items
        Order savedOrder = orderRepository.save(order);

        /*
         * TODO: We need to publish messages as part of the transaction that create order and order items
         *  to prevent inconsistent order state, such as stuck orders management
         *  We could use a simple approach like: Transactional outbox pattern + Polling publisher pattern.
         * */

        // Send stock reservation request to queue
        stockStreamService.requestStockReservation(savedOrder.getId(), createOrderDto.getItems());

        return objectMapper.convertValue(savedOrder, OrderDto.class);
    }

    @Override
    public void cancelOrderItem(UUID orderId, Set<UUID> orderItemIds) {
        Assert.notNull(orderId, "Order ID must not be null");
        Assert.notNull(orderItemIds, "Order item IDs must not be null");

        // Fetch order and order items
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Set<OrderItem> orderItems = order.getOrderItems().stream()
                .filter(i -> orderItemIds.contains(i.getId()))
                .map(orderItem -> {
                    orderItem.setStatus(OrderItemStatus.CANCELLING);
                    return orderItem;
                })
                .collect(Collectors.toSet());

        // Validate that all order items exists
        if (orderItems.size() != orderItemIds.size()) {
            Set<UUID> missingOrderItems = orderItemIds.stream()
                    .filter(id -> !orderItems.contains(id))
                    .collect(Collectors.toSet());

            throw new OrderItemsNotFoundException(orderId, missingOrderItems);
        }

        // Update order and its items
        order.setOrderItems(orderItems);
        orderRepository.save(order);

        // Send stock reservation request to queue
        stockStreamService.requestStockCancellation(orderId, orderItems);
    }
}
