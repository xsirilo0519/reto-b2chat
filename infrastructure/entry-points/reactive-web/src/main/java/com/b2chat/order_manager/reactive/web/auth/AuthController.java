package com.b2chat.order_manager.reactive.web.auth;

import com.b2chat.order_manager.reactive.web.auth.dto.LoginRequestDto;
import com.b2chat.order_manager.reactive.web.auth.dto.LoginResponseDto;
import com.b2chat.order_manager.reactive.web.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequest) {
        if (ADMIN_USERNAME.equals(loginRequest.getUsername())
                && ADMIN_PASSWORD.equals(loginRequest.getPassword())) {

            String token = jwtUtil.generateToken(loginRequest.getUsername());

            return Mono.just(ResponseEntity.ok(
                    LoginResponseDto.builder()
                            .token(token)
                            .type("Bearer")
                            .username(loginRequest.getUsername())
                            .build()
            ));
        }

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}

