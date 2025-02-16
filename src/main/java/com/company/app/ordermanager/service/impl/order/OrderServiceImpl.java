package com.company.app.ordermanager.service.impl.order;

import com.company.app.ordermanager.dto.order.CreateOrderDto;
import com.company.app.ordermanager.dto.order.OrderDto;
import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.entity.orderitem.OrderItem;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.product.Product;
import com.company.app.ordermanager.redis.stream.common.StreamFields;
import com.company.app.ordermanager.redis.stream.common.StreamNames;
import com.company.app.ordermanager.redis.stream.dto.OrderReservationMessage;
import com.company.app.ordermanager.repository.api.order.OrderRepository;
import com.company.app.ordermanager.repository.api.product.ProductRepository;
import com.company.app.ordermanager.service.api.order.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
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

    private final StockReservationService stockReservationService;

    @Override
    @Transactional
    public OrderDto createOrder(CreateOrderDto createOrderDto) {
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
                            .status(OrderItemStatus.PENDING)
                            .build();
                })
                .collect(Collectors.toSet());

        order.setOrderItems(orderItems);

        // Save the order and its items
        Order savedOrder = orderRepository.save(order);

        // Publish reservation message to Redis Stream using Redisson
        try {
            RStream<String, String> stream = redissonClient.getStream(StreamNames.ORDER_RESERVATION.getKey());

            OrderReservationMessage reservationMessage = OrderReservationMessage.builder()
                    .orderId(savedOrder.getId())
                    .items(createOrderDto.getItems())
                    .build();

            String reservationMessageJson = orderMapper.writeValueAsString(reservationMessage);

            // Add message to stream with auto-generated ID
            StreamMessageId messageId = stream.add(StreamAddArgs.entry(StreamFields.MESSAGE.getField(), reservationMessageJson));

            log.debug("Published order reservation message with ID {} for order {}", messageId, savedOrder.getId());
        } catch (JsonProcessingException e) {
            /*
             * Log the error but don't roll back the transaction
             * The order processor will need to implement a mechanism to detect "stuck" orders
             * TODO: Publish messages as part of a transaction that create order and order items to prevent stuck orders managment
             *  Simple approach: Transactional outbox pattern + Polling publisher pattern
             * */
            log.error("Failed to publish reservation message for order {}", savedOrder.getId());
            log.error("Error message: {}", e.getMessage());
        }

        return orderMapper.convertValue(savedOrder, OrderDto.class);
    }
}
