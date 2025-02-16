package com.company.app.ordermanager.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductDto {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockLevel;
}
