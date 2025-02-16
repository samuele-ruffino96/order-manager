package com.company.app.ordermanager.dto.order;

import com.company.app.ordermanager.dto.common.AuditableDto;
import com.company.app.ordermanager.dto.orderitem.OrderItemDto;
import com.company.app.ordermanager.entity.order.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data()
@EqualsAndHashCode(callSuper = true)
public class OrderDto extends AuditableDto {
    private UUID id;
    private String customerName;
    private String description;
    private OrderStatus status;
    private Set<OrderItemDto> items;
    private BigDecimal totalAmount;
}
