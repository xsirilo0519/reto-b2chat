package com.b2chat.order_manager.repository.order.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Table(name = "order_items")
public class OrderItemData {
    @Id
    @Column("id")
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("product_id")
    private Long productId;

    @Column("quantity")
    private Integer quantity;

    @Column("unit_price")
    private BigDecimal unitPrice;

    @Column("total")
    private BigDecimal total;
}

