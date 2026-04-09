package com.b2chat.order_manager.reactive.web.auth;

import com.b2chat.order_manager.reactive.web.auth.dto.LoginRequestDto;
import com.b2chat.order_manager.reactive.web.exception.GlobalExceptionHandler;
import com.b2chat.order_manager.reactive.web.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(jwtUtil);
        webTestClient = WebTestClient.bindToController(authController)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    @Test
    @DisplayName("POST /auth/login - debe retornar 200 OK con token cuando las credenciales son válidas")
    void login_shouldReturnOkWithToken_whenCredentialsAreValid() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("admin");
        request.setPassword("admin");

        when(jwtUtil.generateToken("admin")).thenReturn("mocked-jwt-token");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("mocked-jwt-token")
                .jsonPath("$.type").isEqualTo("Bearer")
                .jsonPath("$.username").isEqualTo("admin");

        verify(jwtUtil).generateToken("admin");
    }

    @Test
    @DisplayName("POST /auth/login - debe retornar 200 OK con campos correctos en el body")
    void login_shouldReturnCorrectBodyFields_whenCredentialsAreValid() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("admin");
        request.setPassword("admin");

        when(jwtUtil.generateToken("admin")).thenReturn("eyJhbGciOiJIUzI1NiJ9.token");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("eyJhbGciOiJIUzI1NiJ9.token")
                .jsonPath("$.type").isEqualTo("Bearer")
                .jsonPath("$.username").isEqualTo("admin");

        verify(jwtUtil).generateToken("admin");
    }

    @Test
    @DisplayName("POST /auth/login - debe retornar 401 UNAUTHORIZED cuando la contraseña es incorrecta")
    void login_shouldReturnUnauthorized_whenPasswordIsWrong() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("admin");
        request.setPassword("wrong-password");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("POST /auth/login - debe retornar 401 UNAUTHORIZED cuando el username es incorrecto")
    void login_shouldReturnUnauthorized_whenUsernameIsWrong() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("wrong-user");
        request.setPassword("admin");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("POST /auth/login - debe retornar 401 UNAUTHORIZED cuando ambas credenciales son incorrectas")
    void login_shouldReturnUnauthorized_whenBothCredentialsAreWrong() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("hacker");
        request.setPassword("hacker123");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("POST /auth/login - debe retornar 401 UNAUTHORIZED cuando las credenciales son nulas")
    void login_shouldReturnUnauthorized_whenCredentialsAreNull() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername(null);
        request.setPassword(null);

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(jwtUtil, never()).generateToken(anyString());
    }
}

