package com.company.app.ordermanager.redis.stream;

import com.company.app.ordermanager.dto.orderitem.CreateOrderItemDto;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;


@Data
@Builder
public class OrderReservationMessage {
    private UUID orderId;
    private Set<CreateOrderItemDto> items;
}
