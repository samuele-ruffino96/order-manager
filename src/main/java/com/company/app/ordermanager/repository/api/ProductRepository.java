package com.company.app.ordermanager.repository.api;

import com.company.app.ordermanager.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}
