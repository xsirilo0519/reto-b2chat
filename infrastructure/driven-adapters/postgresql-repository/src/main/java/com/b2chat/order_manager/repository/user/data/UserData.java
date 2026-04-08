package com.b2chat.order_manager.repository.user.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table(name = "users")
public class UserData {
    @Id
    @Column("id")
    private Long id;
    @Column("name")
    private String name;
    @Column("email")
    private String email;
    @Column("address")
    private String address;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime  updatedAt;
}
