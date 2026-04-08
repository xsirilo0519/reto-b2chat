package com.b2chat.order_manager.domain.users.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserEntity {
    private String id;
    private String name;
    private String email;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime  updatedAt;
}
