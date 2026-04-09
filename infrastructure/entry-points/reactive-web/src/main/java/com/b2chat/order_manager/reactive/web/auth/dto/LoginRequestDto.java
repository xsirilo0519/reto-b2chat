package com.b2chat.order_manager.reactive.web.auth.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String username;
    private String password;
}

