package com.b2chat.order_manager.integration;

import com.b2chat.order_manager.repository.product.data.ProductData;
import com.b2chat.order_manager.repository.user.data.UserData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User/Product integration tests")
class UserProductControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("POST y GET /users deben persistir y recuperar el usuario en PostgreSQL")
    void createAndGetUser_shouldPersistAndReturnUser() {
        Map<String, Object> createdUser = webTestClient.post()
                .uri("/users")
                .bodyValue(Map.of(
                        "name", "Sebastian Diaz",
                        "email", uniqueEmail("user-rest"),
                        "address", "Calle 123 #45-67, Bogotá"
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        Long userId = Long.valueOf(createdUser.get("id").toString());

        webTestClient.get()
                .uri("/users/{id}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId.intValue())
                .jsonPath("$.name").isEqualTo("Sebastian Diaz")
                .jsonPath("$.address").isEqualTo("Calle 123 #45-67, Bogotá");

        UserData persistedUser = userDataRepository.findById(userId).block(Duration.ofSeconds(5));
        assertThat(persistedUser).isNotNull();
        assertThat(persistedUser.getEmail()).isEqualTo(createdUser.get("email"));
    }

    @Test
    @DisplayName("POST, GET, PUT y DELETE /products deben reflejarse correctamente en PostgreSQL")
    void productCrud_shouldBeReflectedInDatabase() {
        Map<String, Object> createdProduct = webTestClient.post()
                .uri("/products")
                .bodyValue(Map.of(
                        "name", "Laptop Dell XPS",
                        "description", "Laptop de alto rendimiento",
                        "price", 2500.00,
                        "stockQuantity", 10
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        Long productId = Long.valueOf(createdProduct.get("id").toString());

        webTestClient.get()
                .uri("/products?page=0")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(productId.intValue())
                .jsonPath("$[0].name").isEqualTo("Laptop Dell XPS");

        ProductData updatedProduct1 = productDataRepository.findById(productId).block(Duration.ofSeconds(5));
        assertThat(updatedProduct1).isNotNull();
        webTestClient.put()
                .uri("/products/{id}", productId)
                .bodyValue(Map.of(
                        "name", "Laptop Dell XPS 15",
                        "description", "Laptop actualizada",
                        "price", 2700.00,
                        "stockQuantity", 8
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(productId.intValue())
                .jsonPath("$.name").isEqualTo("Laptop Dell XPS 15")
                .jsonPath("$.stockQuantity").isEqualTo(8);

        ProductData updatedProduct = productDataRepository.findById(productId).block(Duration.ofSeconds(5));
        assertThat(updatedProduct).isNotNull();
        assertThat(updatedProduct.getName()).isEqualTo("Laptop Dell XPS 15");
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(2700.00));

        webTestClient.delete()
                .uri("/products/{id}", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Producto eliminado correctamente");

        assertThat(productDataRepository.findById(productId).block(Duration.ofSeconds(5))).isNull();
    }
}

