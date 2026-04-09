package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.domain.products.gateway.ProductCacheGateway;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductsUseCase Tests")
class ProductsUseCaseTest {

    @Mock
    private ProductGateway productGateway;

    @Mock
    private ProductCacheGateway productCacheGateway;

    private ProductsUseCase productsUseCase;

    @BeforeEach
    void setUp() {
        productsUseCase = new ProductsUseCase(productGateway, productCacheGateway);
    }

    private ProductEntity buildProduct(Long id) {
        return ProductEntity.builder()
                .id(id)
                .name("Laptop " + id)
                .description("Descripción " + id)
                .price(BigDecimal.valueOf(1000.0))
                .stockQuantity(10)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    private ProductEntity buildProductRequest() {
        return ProductEntity.builder()
                .name("Nuevo Producto")
                .description("Descripción nueva")
                .price(BigDecimal.valueOf(500.0))
                .stockQuantity(20)
                .build();
    }

    // ── getAllProductsUseCase ─────────────────────────────────────────────────

    @Test
    @DisplayName("getAllProductsUseCase - debe retornar la lista de productos de la página indicada")
    void getAllProductsUseCase_shouldReturnProducts_forGivenPage() {
        when(productGateway.findAllProducts(0))
                .thenReturn(Flux.just(buildProduct(1L), buildProduct(2L)));

        StepVerifier.create(productsUseCase.getAllProductsUseCase(0))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(1L))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(2L))
                .verifyComplete();

        verify(productGateway).findAllProducts(0);
    }

    @Test
    @DisplayName("getAllProductsUseCase - debe retornar Flux vacío cuando no hay productos")
    void getAllProductsUseCase_shouldReturnEmptyFlux_whenNoProducts() {
        when(productGateway.findAllProducts(5)).thenReturn(Flux.empty());

        StepVerifier.create(productsUseCase.getAllProductsUseCase(5))
                .verifyComplete();
    }

    // ── getProductByIdUseCase ─────────────────────────────────────────────────

    @Test
    @DisplayName("getProductByIdUseCase - debe retornar el producto desde la caché cuando existe (cache HIT)")
    void getProductByIdUseCase_shouldReturnProductFromCache_whenCacheHit() {
        ProductEntity cached = buildProduct(1L);
        when(productCacheGateway.getProduct(1L)).thenReturn(Mono.just(cached));

        StepVerifier.create(productsUseCase.getProductByIdUseCase(1L))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(1L))
                .verifyComplete();

        verify(productCacheGateway).getProduct(1L);
        verify(productGateway, never()).findProductById(any());
    }

    @Test
    @DisplayName("getProductByIdUseCase - debe consultar BD y guardar en caché cuando no está en caché (cache MISS)")
    void getProductByIdUseCase_shouldFetchFromDbAndSaveInCache_whenCacheMiss() {
        ProductEntity dbProduct = buildProduct(2L);
        when(productCacheGateway.getProduct(2L)).thenReturn(Mono.empty());
        when(productGateway.findProductById(2L)).thenReturn(Mono.just(dbProduct));
        when(productCacheGateway.saveProduct(2L, dbProduct)).thenReturn(Mono.just(dbProduct));

        StepVerifier.create(productsUseCase.getProductByIdUseCase(2L))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(2L))
                .verifyComplete();

        verify(productCacheGateway).getProduct(2L);
        verify(productGateway).findProductById(2L);
        verify(productCacheGateway).saveProduct(2L, dbProduct);
    }

    @Test
    @DisplayName("getProductByIdUseCase - debe propagar error cuando el producto no existe en BD")
    void getProductByIdUseCase_shouldPropagateError_whenProductNotFoundInDb() {
        when(productCacheGateway.getProduct(99L)).thenReturn(Mono.empty());
        when(productGateway.findProductById(99L))
                .thenReturn(Mono.error(new RuntimeException("Producto no encontrado")));

        StepVerifier.create(productsUseCase.getProductByIdUseCase(99L))
                .expectErrorMatches(ex -> ex instanceof RuntimeException)
                .verify();
    }

    // ── createProductUseCase ──────────────────────────────────────────────────

    @Test
    @DisplayName("createProductUseCase - debe añadir timestamps y guardar el producto")
    void createProductUseCase_shouldSetTimestampsAndSave() {
        ProductEntity request = buildProductRequest();
        ProductEntity saved = buildProduct(1L);
        when(productGateway.createProduct(any(ProductEntity.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(productsUseCase.createProductUseCase(request))
                .assertNext(product -> {
                    assertThat(product.getId()).isEqualTo(1L);
                    assertThat(product.getName()).isEqualTo("Laptop 1");
                })
                .verifyComplete();

        verify(productGateway).createProduct(any(ProductEntity.class));
    }

    @Test
    @DisplayName("createProductUseCase - debe enviar el producto con createdAt y updatedAt asignados al gateway")
    void createProductUseCase_shouldPassProductWithTimestampsToGateway() {
        ProductEntity request = buildProductRequest();
        when(productGateway.createProduct(any(ProductEntity.class)))
                .thenAnswer(inv -> Mono.just(((ProductEntity) inv.getArgument(0)).toBuilder().id(1L).build()));

        StepVerifier.create(productsUseCase.createProductUseCase(request))
                .assertNext(product -> {
                    assertThat(product.getCreatedAt()).isNotNull();
                    assertThat(product.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createProductUseCase - debe lanzar RuntimeException cuando el gateway retorna vacío")
    void createProductUseCase_shouldThrowRuntimeException_whenGatewayReturnsEmpty() {
        when(productGateway.createProduct(any(ProductEntity.class))).thenReturn(Mono.empty());

        StepVerifier.create(productsUseCase.createProductUseCase(buildProductRequest()))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && ex.getMessage().equals("Error creating product"))
                .verify();
    }

    // ── updateProductUseCase ──────────────────────────────────────────────────

    @Test
    @DisplayName("updateProductUseCase - debe actualizar el producto y evictar la caché")
    void updateProductUseCase_shouldUpdateProductAndEvictCache() {
        ProductEntity request = buildProductRequest();
        ProductEntity updated = buildProduct(1L);
        when(productGateway.updateProduct(eq(1L), any(ProductEntity.class)))
                .thenReturn(Mono.just(updated));
        when(productCacheGateway.evictProduct(1L)).thenReturn(Mono.empty());

        StepVerifier.create(productsUseCase.updateProductUseCase(1L, request))
                .assertNext(product -> assertThat(product.getId()).isEqualTo(1L))
                .verifyComplete();

        verify(productGateway).updateProduct(eq(1L), any(ProductEntity.class));
        verify(productCacheGateway).evictProduct(1L);
    }

    @Test
    @DisplayName("updateProductUseCase - debe pasar el updatedAt al gateway")
    void updateProductUseCase_shouldPassUpdatedAtToGateway() {
        ProductEntity request = buildProductRequest();
        when(productGateway.updateProduct(eq(1L), any(ProductEntity.class)))
                .thenAnswer(inv -> {
                    ProductEntity arg = inv.getArgument(1);
                    assertThat(arg.getUpdatedAt()).isNotNull();
                    return Mono.just(arg.toBuilder().id(1L).build());
                });
        when(productCacheGateway.evictProduct(1L)).thenReturn(Mono.empty());

        StepVerifier.create(productsUseCase.updateProductUseCase(1L, request))
                .assertNext(product -> assertThat(product.getUpdatedAt()).isNotNull())
                .verifyComplete();
    }

    @Test
    @DisplayName("updateProductUseCase - debe lanzar RuntimeException cuando el producto no existe")
    void updateProductUseCase_shouldThrowRuntimeException_whenProductNotFound() {
        when(productGateway.updateProduct(eq(99L), any(ProductEntity.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(productsUseCase.updateProductUseCase(99L, buildProductRequest()))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && ex.getMessage().contains("99"))
                .verify();
    }

    // ── deleteProductUseCase ──────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProductUseCase - debe eliminar el producto y evictar la caché")
    void deleteProductUseCase_shouldDeleteProductAndEvictCache() {
        when(productGateway.deleteProductById(1L))
                .thenReturn(Mono.just("Producto eliminado correctamente"));
        when(productCacheGateway.evictProduct(1L)).thenReturn(Mono.empty());

        StepVerifier.create(productsUseCase.deleteProductUseCase(1L))
                .assertNext(msg -> assertThat(msg).isEqualTo("Producto eliminado correctamente"))
                .verifyComplete();

        verify(productGateway).deleteProductById(1L);
        verify(productCacheGateway).evictProduct(1L);
    }

    @Test
    @DisplayName("deleteProductUseCase - debe propagar RuntimeException cuando el gateway falla")
    void deleteProductUseCase_shouldPropagateError_whenGatewayFails() {
        when(productGateway.deleteProductById(99L))
                .thenReturn(Mono.error(new RuntimeException("Error deleting product with id: 99")));

        StepVerifier.create(productsUseCase.deleteProductUseCase(99L))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && ex.getMessage().contains("99"))
                .verify();
    }
}

