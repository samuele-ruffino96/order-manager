package com.company.app.ordermanager.service.impl.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.dto.order.OrderDto;
import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.exception.product.ProductNotFoundException;
import com.company.app.ordermanager.exception.product.ProductVersionMismatchException;
import com.company.app.ordermanager.redis.stream.service.api.stock.StockStreamService;
import com.company.app.ordermanager.repository.api.order.OrderRepository;
import com.company.app.ordermanager.repository.api.product.ProductRepository;
import com.company.app.ordermanager.service.api.order.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final StockStreamService stockStreamService;

    @Override
    @Transactional
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
            throw new ProductNotFoundException(missingProducts);
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
        order.setOrderItems(orderItems);

        // Save the order and its items
        Order savedOrder = orderRepository.save(order);

        /*
         * TODO: We need to publish messages as part of the transaction that create order and order items
         *  to prevent inconsistent order state, such as stuck orders management
         *  We could use a simple approach like: Transactional outbox pattern + Polling publisher pattern.
         * */

        // Send stock reservation request to queue
        stockStreamService.sendStockReservationMessage(savedOrder.getOrderItems());

        return objectMapper.convertValue(savedOrder, OrderDto.class);
    }

}
