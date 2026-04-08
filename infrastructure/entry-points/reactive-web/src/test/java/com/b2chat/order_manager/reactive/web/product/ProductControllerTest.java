package com.b2chat.order_manager.reactive.web.product;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.reactive.web.exception.GlobalExceptionHandler;
import com.b2chat.order_manager.reactive.web.product.dto.ProductDto;
import com.b2chat.order_manager.reactive.web.product.dto.ProductResponseDto;
import com.b2chat.order_manager.usecase.ProductsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Mock
    private ProductsUseCase productsUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        ProductController productController = new ProductController(productsUseCase);
        webTestClient = WebTestClient.bindToController(productController)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    @Test
    @DisplayName("GET /products - debe retornar lista de productos con página por defecto")
    void getAllProducts_shouldReturnProductList_withDefaultPage() {
        ProductEntity product1 = ProductEntity.builder()
                .id(1L)
                .name("Laptop")
                .description("High-end laptop")
                .price(BigDecimal.valueOf(1500.0))
                .stockQuantity(10)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();

        ProductEntity product2 = ProductEntity.builder()
                .id(2L)
                .name("Mouse")
                .description("Wireless mouse")
                .price(BigDecimal.valueOf(25.0))
                .stockQuantity(50)
                .createdAt(LocalDateTime.of(2025, 1, 2, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 2, 10, 0))
                .build();

        when(productsUseCase.getAllProductsUseCase(0)).thenReturn(Flux.just(product1, product2));

        webTestClient.get()
                .uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Laptop")
                .jsonPath("$[1].id").isEqualTo(2)
                .jsonPath("$[1].name").isEqualTo("Mouse");

        verify(productsUseCase).getAllProductsUseCase(0);
    }

    @Test
    @DisplayName("GET /products?page=2 - debe retornar lista de productos con la página indicada")
    void getAllProducts_shouldReturnProductList_withCustomPage() {
        ProductEntity product = ProductEntity.builder()
                .id(21L)
                .name("Keyboard")
                .price(BigDecimal.valueOf(80.0))
                .stockQuantity(30)
                .build();

        when(productsUseCase.getAllProductsUseCase(2)).thenReturn(Flux.just(product));

        webTestClient.get()
                .uri("/products?page=2")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponseDto.class)
                .hasSize(1);

        verify(productsUseCase).getAllProductsUseCase(2);
    }

    @Test
    @DisplayName("GET /products - debe retornar lista vacía cuando no hay productos")
    void getAllProducts_shouldReturnEmptyList_whenNoProducts() {
        when(productsUseCase.getAllProductsUseCase(0)).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponseDto.class)
                .hasSize(0);
    }


    @Test
    @DisplayName("POST /products - debe retornar 201 CREATED cuando el producto es válido")
    void createProduct_shouldReturnCreated_whenProductIsValid() {
        ProductDto productDto = ProductDto.builder()
                .name("Nuevo Producto")
                .description("Descripción del producto")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(20)
                .build();

        ProductEntity createdProduct = ProductEntity.builder()
                .id(1L)
                .name("Nuevo Producto")
                .description("Descripción del producto")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(20)
                .createdAt(LocalDateTime.of(2025, 3, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2025, 3, 1, 9, 0))
                .build();

        when(productsUseCase.createProductUseCase(any(ProductEntity.class)))
                .thenReturn(Mono.just(createdProduct));

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Nuevo Producto")
                .jsonPath("$.description").isEqualTo("Descripción del producto")
                .jsonPath("$.price").isEqualTo(99.99)
                .jsonPath("$.stockQuantity").isEqualTo(20);

        verify(productsUseCase).createProductUseCase(any(ProductEntity.class));
    }

    @Test
    @DisplayName("POST /products - debe retornar 400 BAD REQUEST cuando el nombre está vacío")
    void createProduct_shouldReturnBadRequest_whenNameIsBlank() {
        ProductDto productDto = ProductDto.builder()
                .name("")
                .price(BigDecimal.valueOf(50.0))
                .stockQuantity(10)
                .build();

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /products - debe retornar 400 BAD REQUEST cuando el precio es nulo")
    void createProduct_shouldReturnBadRequest_whenPriceIsNull() {
        ProductDto productDto = ProductDto.builder()
                .name("Producto sin precio")
                .price(null)
                .stockQuantity(10)
                .build();

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /products - debe retornar 400 BAD REQUEST cuando el stock es nulo")
    void createProduct_shouldReturnBadRequest_whenStockIsNull() {
        ProductDto productDto = ProductDto.builder()
                .name("Producto sin stock")
                .price(BigDecimal.valueOf(10.0))
                .stockQuantity(null)
                .build();

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /products - debe retornar 400 BAD REQUEST cuando el stock es negativo")
    void createProduct_shouldReturnBadRequest_whenStockIsNegative() {
        ProductDto productDto = ProductDto.builder()
                .name("Producto stock negativo")
                .price(BigDecimal.valueOf(10.0))
                .stockQuantity(-1)
                .build();

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isBadRequest();
    }


    @Test
    @DisplayName("PUT /products/{id} - debe retornar 200 OK con el producto actualizado")
    void updateProduct_shouldReturnUpdatedProduct_whenValid() {
        ProductDto productDto = ProductDto.builder()
                .name("Producto Actualizado")
                .description("Nueva descripción")
                .price(BigDecimal.valueOf(120.0))
                .stockQuantity(15)
                .build();

        ProductEntity updatedProduct = ProductEntity.builder()
                .id(1L)
                .name("Producto Actualizado")
                .description("Nueva descripción")
                .price(BigDecimal.valueOf(120.0))
                .stockQuantity(15)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 4, 1, 10, 0))
                .build();

        when(productsUseCase.updateProductUseCase(eq(1L), any(ProductEntity.class)))
                .thenReturn(Mono.just(updatedProduct));

        webTestClient.put()
                .uri("/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Producto Actualizado")
                .jsonPath("$.price").isEqualTo(120.0)
                .jsonPath("$.stockQuantity").isEqualTo(15);

        verify(productsUseCase).updateProductUseCase(eq(1L), any(ProductEntity.class));
    }

    @Test
    @DisplayName("PUT /products/{id} - debe retornar 400 BAD REQUEST cuando el cuerpo es inválido")
    void updateProduct_shouldReturnBadRequest_whenBodyIsInvalid() {
        ProductDto productDto = ProductDto.builder()
                .name("")
                .price(BigDecimal.valueOf(10.0))
                .stockQuantity(5)
                .build();

        webTestClient.put()
                .uri("/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isBadRequest();
    }


    @Test
    @DisplayName("DELETE /products/{id} - debe retornar 200 OK con mensaje de confirmación")
    void deleteProduct_shouldReturnOk_withConfirmationMessage() {
        when(productsUseCase.deleteProductUseCase(1L))
                .thenReturn(Mono.just("Producto eliminado exitosamente"));

        webTestClient.delete()
                .uri("/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Producto eliminado exitosamente");

        verify(productsUseCase).deleteProductUseCase(1L);
    }

    @Test
    @DisplayName("DELETE /products/{id} - debe propagar el mensaje del use case")
    void deleteProduct_shouldPropagateUseCaseMessage() {
        String expectedMessage = "Product with id 5 deleted";
        when(productsUseCase.deleteProductUseCase(5L))
                .thenReturn(Mono.just(expectedMessage));

        webTestClient.delete()
                .uri("/products/5")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedMessage);

        verify(productsUseCase).deleteProductUseCase(5L);
    }


    @Test
    @DisplayName("PUT /products/{id} - debe retornar 500 cuando el use case lanza RuntimeException (handleGenericError)")
    void updateProduct_shouldReturn500_whenUseCaseThrowsRuntimeException() {
        ProductDto productDto = ProductDto.builder()
                .name("Producto Válido")
                .price(BigDecimal.valueOf(50.0))
                .stockQuantity(10)
                .build();

        when(productsUseCase.updateProductUseCase(eq(99L), any(ProductEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("Product not found with id: 99")));

        webTestClient.put()
                .uri("/products/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.message").isEqualTo("Error interno del servidor");
    }

    @Test
    @DisplayName("DELETE /products/{id} - debe retornar 500 cuando el use case lanza RuntimeException (handleGenericError)")
    void deleteProduct_shouldReturn500_whenUseCaseThrowsRuntimeException() {
        when(productsUseCase.deleteProductUseCase(99L))
                .thenReturn(Mono.error(new RuntimeException("Error deleting product with id: 99")));

        webTestClient.delete()
                .uri("/products/99")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.message").isEqualTo("Error interno del servidor");

        verify(productsUseCase).deleteProductUseCase(99L);
    }

    @Test
    @DisplayName("POST /products - debe retornar 500 cuando el use case lanza RuntimeException (handleGenericError)")
    void createProduct_shouldReturn500_whenUseCaseThrowsRuntimeException() {
        ProductDto productDto = ProductDto.builder()
                .name("Producto Válido")
                .price(BigDecimal.valueOf(50.0))
                .stockQuantity(10)
                .build();

        when(productsUseCase.createProductUseCase(any(ProductEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("Error creating product")));

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.message").isEqualTo("Error interno del servidor");
    }
}



