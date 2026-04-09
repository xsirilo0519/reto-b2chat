package com.b2chat.order_manager.cache.product.adapter;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductCacheAdapter Tests")
class ProductCacheAdapterTest {

    @Mock
    private ReactiveRedisTemplate<String, ProductEntity> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, ProductEntity> valueOperations;

    private ProductCacheAdapter productCacheAdapter;

    private static final String PREFIX = "product:";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        productCacheAdapter = new ProductCacheAdapter(redisTemplate);
    }

    private ProductEntity buildProduct(Long id) {
        return ProductEntity.builder()
                .id(id)
                .name("Producto " + id)
                .description("Descripción " + id)
                .price(BigDecimal.valueOf(100.0))
                .stockQuantity(5)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    // ── getProduct ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProduct - debe retornar el producto cuando existe en caché (cache HIT)")
    void getProduct_shouldReturnProduct_whenCacheHit() {
        ProductEntity product = buildProduct(1L);
        when(valueOperations.get(PREFIX + "1")).thenReturn(Mono.just(product));

        StepVerifier.create(productCacheAdapter.getProduct(1L))
                .assertNext(p -> {
                    assertThat(p.getId()).isEqualTo(1L);
                    assertThat(p.getName()).isEqualTo("Producto 1");
                })
                .verifyComplete();

        verify(valueOperations).get(PREFIX + "1");
    }

    @Test
    @DisplayName("getProduct - debe retornar Mono vacío cuando no existe en caché (cache MISS)")
    void getProduct_shouldReturnEmpty_whenCacheMiss() {
        when(valueOperations.get(PREFIX + "99")).thenReturn(Mono.empty());

        StepVerifier.create(productCacheAdapter.getProduct(99L))
                .verifyComplete();

        verify(valueOperations).get(PREFIX + "99");
    }

    @Test
    @DisplayName("getProduct - debe construir la clave con el prefijo 'product:'")
    void getProduct_shouldBuildKeyWithCorrectPrefix() {
        when(valueOperations.get("product:42")).thenReturn(Mono.just(buildProduct(42L)));

        StepVerifier.create(productCacheAdapter.getProduct(42L))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(42L))
                .verifyComplete();

        verify(valueOperations).get("product:42");
    }

    // ── saveProduct ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveProduct - debe guardar el producto en caché y retornarlo")
    void saveProduct_shouldSaveAndReturnProduct() {
        ProductEntity product = buildProduct(1L);
        when(valueOperations.set(eq(PREFIX + "1"), eq(product), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(productCacheAdapter.saveProduct(1L, product))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(1L))
                .verifyComplete();

        verify(valueOperations).set(eq(PREFIX + "1"), eq(product), any(Duration.class));
    }

    @Test
    @DisplayName("saveProduct - debe guardar con TTL de exactamente 10 minutos")
    void saveProduct_shouldUseCorrectTtlOf10Minutes() {
        ProductEntity product = buildProduct(2L);
        when(valueOperations.set(eq(PREFIX + "2"), eq(product), eq(Duration.ofMinutes(10))))
                .thenReturn(Mono.just(true));

        StepVerifier.create(productCacheAdapter.saveProduct(2L, product))
                .assertNext(p -> assertThat(p).isEqualTo(product))
                .verifyComplete();

        verify(valueOperations).set(PREFIX + "2", product, Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("saveProduct - debe retornar el mismo producto recibido")
    void saveProduct_shouldReturnSameProductReceived() {
        ProductEntity product = buildProduct(5L);
        when(valueOperations.set(eq(PREFIX + "5"), eq(product), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(productCacheAdapter.saveProduct(5L, product))
                .assertNext(p -> assertThat(p).isEqualTo(product))
                .verifyComplete();
    }

    // ── evictProduct ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("evictProduct - debe eliminar la clave del producto de la caché")
    void evictProduct_shouldDeleteProductKey() {
        when(valueOperations.delete(PREFIX + "1")).thenReturn(Mono.just(true));

        StepVerifier.create(productCacheAdapter.evictProduct(1L))
                .verifyComplete();

        verify(valueOperations).delete(PREFIX + "1");
    }

    @Test
    @DisplayName("evictProduct - debe completar sin error aunque la clave no exista")
    void evictProduct_shouldCompleteSuccessfully_whenKeyDoesNotExist() {
        when(valueOperations.delete(PREFIX + "99")).thenReturn(Mono.just(false));

        StepVerifier.create(productCacheAdapter.evictProduct(99L))
                .verifyComplete();
    }

    @Test
    @DisplayName("evictProduct - debe usar el prefijo correcto al construir la clave")
    void evictProduct_shouldBuildKeyWithCorrectPrefix() {
        when(valueOperations.delete("product:7")).thenReturn(Mono.just(true));

        StepVerifier.create(productCacheAdapter.evictProduct(7L))
                .verifyComplete();

        verify(valueOperations).delete("product:7");
    }
}

