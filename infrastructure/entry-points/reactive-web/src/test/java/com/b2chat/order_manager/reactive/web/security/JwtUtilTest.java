package com.b2chat.order_manager.reactive.web.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "your-secret-key-that-is-long-enough-for-hs256");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    @DisplayName("generateToken - debe generar un token no nulo para un usuario válido")
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken("testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("extractUsername - debe extraer el username correcto del token generado")
    void extractUsername_shouldReturnCorrectUsername() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        assertEquals(username, jwtUtil.extractUsername(token));
    }

    @Test
    @DisplayName("isTokenValid - debe retornar true para un token recién generado")
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {
        String token = jwtUtil.generateToken("testuser");
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid - debe retornar false para un token manipulado")
    void isTokenValid_shouldReturnFalse_whenTokenIsTampered() {
        String token = jwtUtil.generateToken("testuser");
        assertFalse(jwtUtil.isTokenValid(token + "tampered"));
    }

    @Test
    @DisplayName("isTokenValid - debe retornar false para un token vacío")
    void isTokenValid_shouldReturnFalse_whenTokenIsEmpty() {
        assertFalse(jwtUtil.isTokenValid(""));
    }

    @Test
    @DisplayName("generateToken - tokens distintos para el mismo username deben ser válidos")
    void generateToken_multipleTokens_shouldAllBeValid() {
        String token1 = jwtUtil.generateToken("admin");
        String token2 = jwtUtil.generateToken("admin");
        assertTrue(jwtUtil.isTokenValid(token1));
        assertTrue(jwtUtil.isTokenValid(token2));
    }
}
