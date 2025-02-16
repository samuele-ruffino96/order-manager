package com.company.app.ordermanager.repository.api.product;

import com.company.app.ordermanager.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
}
