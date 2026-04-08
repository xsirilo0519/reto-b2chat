package com.b2chat.order_manager.reactive.web.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ProductDto {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
}

