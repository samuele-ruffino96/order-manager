package com.company.app.ordermanager.search.service.api;

import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.search.dto.OrderSearchRequest;
import com.company.app.ordermanager.search.dto.OrderSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderSearchService {
    Page<OrderSearchResult> searchOrders(OrderSearchRequest searchRequest, Pageable pageable);

    void indexOrder(Order order);

    void updateOrder(Order order);

    void deleteOrder(UUID orderId);
}
