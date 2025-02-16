package com.company.app.ordermanager.repository.api.orderitem;

import com.company.app.ordermanager.entity.orderitem.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
