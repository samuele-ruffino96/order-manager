package com.company.app.ordermanager.repository.api;

import com.company.app.ordermanager.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
