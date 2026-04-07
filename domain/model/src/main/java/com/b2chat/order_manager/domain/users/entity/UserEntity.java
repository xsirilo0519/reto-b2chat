package com.b2chat.order_manager.domain.users.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class UserEntity {
    private String name;
    private String email;
    private String address;
}
