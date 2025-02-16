package com.company.app.ordermanager.repository.api.order;

import com.company.app.ordermanager.entity.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
