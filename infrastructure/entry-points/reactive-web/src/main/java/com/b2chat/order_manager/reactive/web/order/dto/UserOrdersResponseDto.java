package com.b2chat.order_manager.reactive.web.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserOrdersResponseDto {
    private Long userId;
    private int totalOrders;
    private List<OrderSummaryDto> orders;
}

