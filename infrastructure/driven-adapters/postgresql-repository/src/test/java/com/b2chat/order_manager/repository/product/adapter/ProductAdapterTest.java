package com.b2chat.order_manager.repository.product.adapter;

import com.b2chat.order_manager.domain.exception.InsufficientStockException;
import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.repository.product.data.ProductData;
import com.b2chat.order_manager.repository.product.repository.ProductDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductAdapter Tests")
class ProductAdapterTest {

    @Mock
    private ProductDataRepository productDataRepository;

    private ProductAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductAdapter(productDataRepository);
    }


    private ProductData buildProductData(Long id) {
        ProductData data = new ProductData();
        data.setId(id);
        data.setName("Laptop");
        data.setDescription("High-end laptop");
        data.setPrice(BigDecimal.valueOf(1500.0));
        data.setStockQuantity(10);
        data.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        data.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return data;
    }

    private ProductEntity buildProductEntity() {
        return ProductEntity.builder()
                .name("Laptop")
                .description("High-end laptop")
                .price(BigDecimal.valueOf(1500.0))
                .stockQuantity(10)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }


    @Test
    @DisplayName("findProductById - debe retornar el producto cuando existe")
    void findProductById_shouldReturnProduct_whenFound() {
        when(productDataRepository.findById(1L)).thenReturn(Mono.just(buildProductData(1L)));

        StepVerifier.create(adapter.findProductById(1L))
                .assertNext(product -> {
                    assertThat(product.getId()).isEqualTo(1L);
                    assertThat(product.getName()).isEqualTo("Laptop");
                    assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1500.0));
                    assertThat(product.getStockQuantity()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findProductById - debe lanzar ResourceNotFoundException cuando no existe")
    void findProductById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(productDataRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findProductById(99L))
                .expectErrorMatches(ex ->
                        ex instanceof ResourceNotFoundException &&
                        ex.getMessage().contains("99"))
                .verify();
    }


    @Test
    @DisplayName("decrementStock - debe completar correctamente cuando el stock se decrementa")
    void decrementStock_shouldComplete_whenDecrementSucceeds() {
        when(productDataRepository.decrementStockQuery(1L, 2)).thenReturn(Mono.just(1));

        StepVerifier.create(adapter.decrementStock(1L, 2))
                .verifyComplete();

        verify(productDataRepository).decrementStockQuery(1L, 2);
    }

    @Test
    @DisplayName("decrementStock - debe lanzar ResourceNotFoundException cuando no se actualizó ninguna fila (producto no existe)")
    void decrementStock_shouldThrowResourceNotFoundException_whenNoRowsUpdated() {
        when(productDataRepository.decrementStockQuery(99L, 2)).thenReturn(Mono.just(0));

        StepVerifier.create(adapter.decrementStock(99L, 2))
                .expectErrorMatches(ex ->
                        ex instanceof ResourceNotFoundException &&
                        ex.getMessage().contains("99"))
                .verify();
    }

    @Test
    @DisplayName("decrementStock - debe lanzar InsufficientStockException cuando viola restricción de integridad (stock negativo)")
    void decrementStock_shouldThrowInsufficientStockException_whenDataIntegrityViolation() {
        when(productDataRepository.decrementStockQuery(1L, 5))
                .thenReturn(Mono.error(new DataIntegrityViolationException("stock_quantity check violation")));

        StepVerifier.create(adapter.decrementStock(1L, 5))
                .expectErrorMatches(ex ->
                        ex instanceof InsufficientStockException &&
                        ex.getMessage().contains("producto id 1"))
                .verify();
    }


    @Test
    @DisplayName("findAllProducts - debe retornar la lista paginada de productos")
    void findAllProducts_shouldReturnPagedProducts() {
        when(productDataRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(buildProductData(1L), buildProductData(2L)));

        StepVerifier.create(adapter.findAllProducts(0))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(1L))
                .assertNext(p -> assertThat(p.getId()).isEqualTo(2L))
                .verifyComplete();

        verify(productDataRepository).findAllBy(any(Pageable.class));
    }

    @Test
    @DisplayName("findAllProducts - debe retornar lista vacía cuando no hay productos en la página")
    void findAllProducts_shouldReturnEmptyFlux_whenNoProducts() {
        when(productDataRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.empty());

        StepVerifier.create(adapter.findAllProducts(5))
                .verifyComplete();
    }


    @Test
    @DisplayName("createProduct - debe guardar y retornar el producto creado")
    void createProduct_shouldSaveAndReturnProduct() {
        ProductData savedData = buildProductData(1L);
        when(productDataRepository.save(any(ProductData.class))).thenReturn(Mono.just(savedData));

        StepVerifier.create(adapter.createProduct(buildProductEntity()))
                .assertNext(product -> {
                    assertThat(product.getId()).isEqualTo(1L);
                    assertThat(product.getName()).isEqualTo("Laptop");
                })
                .verifyComplete();

        verify(productDataRepository).save(any(ProductData.class));
    }


    @Test
    @DisplayName("updateProduct - debe actualizar los campos y retornar el producto actualizado")
    void updateProduct_shouldUpdateFieldsAndReturnUpdatedProduct() {
        ProductData existing = buildProductData(1L);
        ProductEntity updatedEntity = ProductEntity.builder()
                .name("Laptop Pro")
                .description("Premium laptop")
                .price(BigDecimal.valueOf(2000.0))
                .stockQuantity(5)
                .updatedAt(LocalDateTime.of(2025, 6, 1, 12, 0))
                .build();

        ProductData savedData = buildProductData(1L);
        savedData.setName("Laptop Pro");
        savedData.setPrice(BigDecimal.valueOf(2000.0));
        savedData.setStockQuantity(5);

        when(productDataRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(productDataRepository.save(any(ProductData.class))).thenReturn(Mono.just(savedData));

        StepVerifier.create(adapter.updateProduct(1L, updatedEntity))
                .assertNext(product -> {
                    assertThat(product.getId()).isEqualTo(1L);
                    assertThat(product.getName()).isEqualTo("Laptop Pro");
                    assertThat(product.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2000.0));
                    assertThat(product.getStockQuantity()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateProduct - debe verificar que los campos correctos son asignados al objeto existente")
    void updateProduct_shouldMapAllFieldsToExistingData() {
        ProductData existing = buildProductData(1L);
        ArgumentCaptor<ProductData> captor = ArgumentCaptor.forClass(ProductData.class);
        ProductEntity updatedEntity = ProductEntity.builder()
                .name("Mouse")
                .description("Wireless mouse")
                .price(BigDecimal.valueOf(25.0))
                .stockQuantity(50)
                .updatedAt(LocalDateTime.of(2025, 6, 1, 12, 0))
                .build();

        when(productDataRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(productDataRepository.save(captor.capture())).thenReturn(Mono.just(existing));

        StepVerifier.create(adapter.updateProduct(1L, updatedEntity))
                .assertNext(p -> assertThat(p).isNotNull())
                .verifyComplete();

        ProductData saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Mouse");
        assertThat(saved.getDescription()).isEqualTo("Wireless mouse");
        assertThat(saved.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(25.0));
        assertThat(saved.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("updateProduct - debe lanzar ResourceNotFoundException cuando el producto no existe")
    void updateProduct_shouldThrowResourceNotFoundException_whenNotFound() {
        when(productDataRepository.findById(eq(99L))).thenReturn(Mono.empty());

        StepVerifier.create(adapter.updateProduct(99L, buildProductEntity()))
                .expectErrorMatches(ex ->
                        ex instanceof ResourceNotFoundException &&
                        ex.getMessage().contains("99"))
                .verify();
    }


    @Test
    @DisplayName("deleteProductById - debe retornar mensaje de confirmación cuando el producto existe")
    void deleteProductById_shouldReturnConfirmationMessage_whenFound() {
        when(productDataRepository.findById(1L)).thenReturn(Mono.just(buildProductData(1L)));
        when(productDataRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.deleteProductById(1L))
                .assertNext(msg -> assertThat(msg).isEqualTo("Producto eliminado correctamente"))
                .verifyComplete();

        verify(productDataRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteProductById - debe lanzar ResourceNotFoundException cuando el producto no existe")
    void deleteProductById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(productDataRepository.findById(eq(99L))).thenReturn(Mono.empty());

        StepVerifier.create(adapter.deleteProductById(99L))
                .expectErrorMatches(ex ->
                        ex instanceof ResourceNotFoundException &&
                        ex.getMessage().contains("99"))
                .verify();
    }
}

