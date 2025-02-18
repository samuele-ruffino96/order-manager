package com.company.app.ordermanager.dto.orderitem;


import com.company.app.ordermanager.dto.product.ProductDto;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatus;
import com.company.app.ordermanager.entity.orderitem.OrderItemStatusReason;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemDto {
    private UUID id;
    private ProductDto product;
    private int quantity;
    private BigDecimal purchasePrice;
    private OrderItemStatus status;
    private OrderItemStatusReason reason;
}


