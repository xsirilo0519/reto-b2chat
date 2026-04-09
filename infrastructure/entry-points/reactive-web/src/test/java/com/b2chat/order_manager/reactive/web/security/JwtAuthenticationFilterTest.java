package com.b2chat.order_manager.reactive.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WebFilterChain webFilterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("filter - sin header Authorization debe continuar la cadena sin validar token")
    void filter_withNoAuthHeader_shouldContinueChainWithoutValidatingToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(webFilterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, webFilterChain))
                .verifyComplete();

        verify(webFilterChain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("filter - con header sin prefijo Bearer debe continuar sin validar token")
    void filter_withHeaderWithoutBearerPrefix_shouldContinueChainWithoutValidatingToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(webFilterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, webFilterChain))
                .verifyComplete();

        verify(webFilterChain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("filter - con Bearer token inválido debe continuar la cadena sin autenticar")
    void filter_withInvalidBearerToken_shouldContinueChainWithoutAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);
        when(webFilterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, webFilterChain))
                .verifyComplete();

        verify(jwtUtil).isTokenValid("invalid-token");
        verify(webFilterChain).filter(exchange);
    }

    @Test
    @DisplayName("filter - con Bearer token válido debe autenticar al usuario y continuar la cadena")
    void filter_withValidBearerToken_shouldAuthenticateAndContinueChain() {
        String token = "valid-token";
        String username = "testuser";
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(webFilterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, webFilterChain))
                .verifyComplete();

        verify(jwtUtil).isTokenValid(token);
        verify(jwtUtil).extractUsername(token);
        verify(webFilterChain).filter(exchange);
    }
}
