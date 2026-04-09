package com.b2chat.order_manager.reactive.web.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemSummaryDto {
    private String productName;
    private String productDescription;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}

