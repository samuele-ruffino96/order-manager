package com.company.app.ordermanager.dto.order;

import com.company.app.ordermanager.dto.common.AuditableDto;
import com.company.app.ordermanager.dto.orderitem.OrderItemDto;
import com.company.app.ordermanager.entity.order.OrderStatus;
import com.company.app.ordermanager.view.OrderViews;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data()
@EqualsAndHashCode(callSuper = true)
@JsonView(OrderViews.ListView.class)
public class OrderDto extends AuditableDto {
    private UUID id;
    private String customerName;
    private String description;
    private OrderStatus status;

    @JsonView(OrderViews.DetailView.class)
    private Set<OrderItemDto> items;

    private BigDecimal totalAmount;
}
