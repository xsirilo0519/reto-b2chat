package com.b2chat.order_manager.integration;

import com.b2chat.order_manager.repository.order.data.OrderData;
import com.b2chat.order_manager.repository.order.data.OrderItemData;
import com.b2chat.order_manager.repository.product.data.ProductData;
import com.b2chat.order_manager.repository.user.data.UserData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("Order flow integration tests")
class OrderFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("POST /orders debe publicar en RabbitMQ, persistir la orden y enviar email de recibido")
    void createOrder_shouldPublishPersistAndSendReceivedEmail() {
        String email = uniqueEmail("order-received");
        UserData user = persistUser("Cliente Recibido", email, "Cra 10 #20-30");
        ProductData product = persistProduct("Laptop", "Laptop gamer", BigDecimal.valueOf(1250.00), 5);

        webTestClient.post()
                .uri("/orders")
                .bodyValue(Map.of(
                        "userId", user.getId(),
                        "items", List.of(Map.of(
                                "productId", product.getId(),
                                "quantity", 2
                        ))
                ))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Pedido recibido. Será procesado en breve.");

        AtomicReference<OrderData> savedOrderRef = new AtomicReference<>();
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<OrderData> orders = orderDataRepository.findAll().collectList().block(Duration.ofSeconds(5));
                    assertThat(orders).hasSize(1);
                    savedOrderRef.set(orders.get(0));
                    assertThat(orders.get(0).getStatus()).isEqualTo("PENDING");
                    assertThat(orders.get(0).getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));
                });

        OrderData savedOrder = savedOrderRef.get();
        List<OrderItemData> savedItems = orderItemDataRepository.findByOrderId(savedOrder.getId())
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getProductId()).isEqualTo(product.getId());
        assertThat(savedItems.get(0).getQuantity()).isEqualTo(2);
        assertThat(savedItems.get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1250.00));
        assertThat(savedItems.get(0).getTotal()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));

        webTestClient.get()
                .uri("/orders/{id}", savedOrder.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(savedOrder.getId().intValue())
                .jsonPath("$.status").isEqualTo("PENDING")
                .jsonPath("$.totalAmount").isEqualTo(2500.00)
                .jsonPath("$.items[0].quantity").isEqualTo(2);

        awaitEmail(email, "Pedido #" + savedOrder.getId() + " recibido");
    }

    @Test
    @DisplayName("PUT /orders/{id}/status debe completar la orden, descontar stock y enviar email")
    void updateOrderStatus_shouldCompleteOrderDecreaseStockAndSendEmail() {
        String email = uniqueEmail("order-completed");
        UserData user = persistUser("Cliente Completado", email, "Av 15 #10-20");
        ProductData product = persistProduct("Mouse", "Mouse inalámbrico", BigDecimal.valueOf(80.00), 5);
        OrderData order = persistOrder(user.getId(), "PENDING", BigDecimal.valueOf(160.00), false);
        persistOrderItem(order.getId(), product.getId(), 2, BigDecimal.valueOf(80.00), BigDecimal.valueOf(160.00));

        webTestClient.put()
                .uri("/orders/{id}/status", order.getId())
                .bodyValue(Map.of("status", "PROCESSING"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(order.getId().intValue())
                .jsonPath("$.status").isEqualTo("PROCESSING");

        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    OrderData processedOrder = orderDataRepository.findById(order.getId()).block(Duration.ofSeconds(5));
                    ProductData updatedProduct = productDataRepository.findById(product.getId()).block(Duration.ofSeconds(5));

                    assertThat(processedOrder).isNotNull();
                    assertThat(processedOrder.getStatus()).isEqualTo("COMPLETED");
                    assertThat(processedOrder.isCompleted()).isTrue();
                    assertThat(updatedProduct).isNotNull();
                    assertThat(updatedProduct.getStockQuantity()).isEqualTo(3);
                });

        webTestClient.get()
                .uri("/orders/{id}", order.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED");

        awaitEmail(email, "Pedido #" + order.getId() + " completado");
    }

    @Test
    @DisplayName("PUT /orders/{id}/status debe cancelar la orden cuando no hay stock y enviar email")
    void updateOrderStatus_shouldCancelOrderWhenStockIsInsufficient() {
        String email = uniqueEmail("order-cancelled");
        UserData user = persistUser("Cliente Cancelado", email, "Calle 8 #99-10");
        ProductData product = persistProduct("Teclado", "Teclado mecánico", BigDecimal.valueOf(150.00), 1);
        OrderData order = persistOrder(user.getId(), "PENDING", BigDecimal.valueOf(750.00), false);
        persistOrderItem(order.getId(), product.getId(), 5, BigDecimal.valueOf(150.00), BigDecimal.valueOf(750.00));

        webTestClient.put()
                .uri("/orders/{id}/status", order.getId())
                .bodyValue(Map.of("status", "PROCESSING"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PROCESSING");

        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    OrderData cancelledOrder = orderDataRepository.findById(order.getId()).block(Duration.ofSeconds(5));
                    ProductData unchangedProduct = productDataRepository.findById(product.getId()).block(Duration.ofSeconds(5));

                    assertThat(cancelledOrder).isNotNull();
                    assertThat(cancelledOrder.getStatus()).isEqualTo("CANCELLED");
                    assertThat(cancelledOrder.isCompleted()).isFalse();
                    assertThat(unchangedProduct).isNotNull();
                    assertThat(unchangedProduct.getStockQuantity()).isEqualTo(1);
                });

        webTestClient.get()
                .uri("/orders/{id}", order.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("CANCELLED");

        awaitEmail(email, "Pedido #" + order.getId() + " cancelado");
    }
}

