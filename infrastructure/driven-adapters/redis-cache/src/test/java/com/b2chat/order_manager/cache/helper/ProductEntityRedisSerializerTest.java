package com.b2chat.order_manager.cache.helper;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductEntityRedisSerializer Tests")
class ProductEntityRedisSerializerTest {

    private ProductEntityRedisSerializer serializer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serializer = new ProductEntityRedisSerializer(objectMapper);
    }

    private ProductEntity buildProduct() {
        return ProductEntity.builder()
                .id(1L)
                .name("Laptop Dell")
                .description("Laptop de alto rendimiento")
                .price(BigDecimal.valueOf(2500.0))
                .stockQuantity(10)
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 3, 20, 14, 30))
                .build();
    }

    // ── serialize ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("serialize - debe retornar bytes JSON no nulos cuando el producto es válido")
    void serialize_shouldReturnNonNullBytes_whenProductIsNotNull() {
        byte[] result = serializer.serialize(buildProduct());
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("serialize - debe retornar null cuando el producto es null")
    void serialize_shouldReturnNull_whenProductIsNull() {
        byte[] result = serializer.serialize(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("serialize - los bytes generados deben contener el nombre del producto")
    void serialize_shouldContainProductNameInJson() {
        byte[] result = serializer.serialize(buildProduct());
        String json = new String(result);
        assertThat(json).contains("Laptop Dell");
    }

    // ── deserialize ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deserialize - debe reconstruir el producto con todos los campos correctos")
    void deserialize_shouldReturnProductWithCorrectFields() {
        ProductEntity original = buildProduct();
        byte[] bytes = serializer.serialize(original);

        ProductEntity result = serializer.deserialize(bytes);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop Dell");
        assertThat(result.getDescription()).isEqualTo("Laptop de alto rendimiento");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2500.0));
        assertThat(result.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("deserialize - debe retornar null cuando los bytes son null")
    void deserialize_shouldReturnNull_whenBytesAreNull() {
        ProductEntity result = serializer.deserialize(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("deserialize - debe retornar null cuando los bytes están vacíos")
    void deserialize_shouldReturnNull_whenBytesAreEmpty() {
        ProductEntity result = serializer.deserialize(new byte[0]);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("serialize/deserialize - roundtrip debe preservar los LocalDateTime correctamente")
    void serializeDeserialize_shouldPreserveLocalDateTime() {
        ProductEntity original = buildProduct();
        byte[] bytes = serializer.serialize(original);
        ProductEntity result = serializer.deserialize(bytes);

        assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 0));
        assertThat(result.getUpdatedAt()).isEqualTo(LocalDateTime.of(2025, 3, 20, 14, 30));
    }

    @Test
    @DisplayName("deserialize - debe lanzar SerializationException con bytes JSON inválidos")
    void deserialize_shouldThrowSerializationException_whenBytesAreInvalid() {
        byte[] invalidBytes = "esto no es json".getBytes();
        assertThatThrownBy(() -> serializer.deserialize(invalidBytes))
                .isInstanceOf(SerializationException.class);
    }
}

