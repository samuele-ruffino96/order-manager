package com.company.app.ordermanager.dto.orderitem;


import com.company.app.ordermanager.dto.product.ProductDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemDto {
    private UUID id;
    private ProductDto product;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal subtotal;
}


