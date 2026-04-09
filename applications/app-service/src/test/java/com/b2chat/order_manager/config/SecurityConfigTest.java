package com.b2chat.order_manager.config;

import com.b2chat.order_manager.reactive.web.security.JwtAuthenticationFilter;
import com.b2chat.order_manager.reactive.web.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private JwtUtil jwtUtil;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil);
        webTestClient = WebTestClient
                .bindToWebHandler(exchange -> exchange.getResponse().setComplete())
                .webFilter(jwtFilter)
                .build();
    }

    @Test
    @DisplayName("Request sin Authorization debe pasar la cadena sin intentar validar token")
    void request_withNoAuthHeader_shouldPassWithoutValidatingToken() {
        webTestClient.get().uri("/any-path")
                .exchange()
                .expectStatus().isOk();

        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("Request con header sin prefijo Bearer no debe validar token")
    void request_withHeaderWithoutBearerPrefix_shouldNotValidateToken() {
        webTestClient.get().uri("/any-path")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .exchange()
                .expectStatus().isOk();

        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("Request con Bearer token válido debe autenticar al usuario")
    void request_withValidBearerToken_shouldAuthenticateUser() {
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");

        webTestClient.get().uri("/any-path")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .exchange()
                .expectStatus().isOk();

        verify(jwtUtil).isTokenValid("valid-token");
        verify(jwtUtil).extractUsername("valid-token");
    }

    @Test
    @DisplayName("Request con Bearer token inválido debe continuar sin autenticar")
    void request_withInvalidBearerToken_shouldContinueWithoutAuthentication() {
        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

        webTestClient.get().uri("/any-path")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .exchange()
                .expectStatus().isOk();

        verify(jwtUtil).isTokenValid("invalid-token");
        verify(jwtUtil, never()).extractUsername(any());
    }
}
