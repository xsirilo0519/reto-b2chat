package com.b2chat.order_manager.repository.product.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table(name = "products")
public class ProductData {
    @Id
    @Column("id")
    private Long id;
    @Column("name")
    private String name;
    @Column("description")
    private String description;
    @Column("price")
    private BigDecimal price;
    @Column("stock_quantity")
    private Integer stockQuantity;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}

