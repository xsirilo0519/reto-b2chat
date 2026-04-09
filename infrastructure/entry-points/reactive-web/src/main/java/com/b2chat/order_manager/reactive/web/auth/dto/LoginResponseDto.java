package com.b2chat.order_manager.reactive.web.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDto {
    private String token;
    private String type;
    private String username;
}

