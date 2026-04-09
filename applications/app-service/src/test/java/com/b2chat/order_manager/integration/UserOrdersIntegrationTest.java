package com.b2chat.order_manager.integration;

import com.b2chat.order_manager.repository.order.data.OrderData;
import com.b2chat.order_manager.repository.product.data.ProductData;
import com.b2chat.order_manager.repository.user.data.UserData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@DisplayName("GET /users/{userId}/orders — Integration Tests")
class UserOrdersIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("debe retornar 200 con la lista completa de pedidos y detalles de productos via INNER JOIN")
    void getOrdersByUserId_shouldReturnOrdersWithProductDetails_viaInnerJoin() {
        UserData user   = persistUser("Usuario Join", uniqueEmail("join-test"), "Calle Join");
        ProductData laptop = persistProduct("Laptop Pro", "Descripción de Laptop Pro", new BigDecimal("1500.00"), 20);
        ProductData mouse  = persistProduct("Mouse Inalámbrico", "Descripción de Mouse Inalámbrico", new BigDecimal("35.00"), 20);

        OrderData order1 = persistOrder(user.getId(), "COMPLETED", new BigDecimal("1500.00"), true);
        persistOrderItem(order1.getId(), laptop.getId(), 1, new BigDecimal("1500.00"), new BigDecimal("1500.00"));

        OrderData order2 = persistOrder(user.getId(), "PENDING", new BigDecimal("70.00"), false);
        persistOrderItem(order2.getId(), mouse.getId(), 2, new BigDecimal("35.00"), new BigDecimal("70.00"));

        webTestClient.get()
                .uri("/users/{userId}/orders", user.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(user.getId().intValue())
                .jsonPath("$.totalOrders").isEqualTo(2)
                .jsonPath("$.orders.length()").isEqualTo(2)
                // primer pedido (más reciente primero — ORDER BY created_at DESC)
                .jsonPath("$.orders[0].items[0].productName").isEqualTo("Mouse Inalámbrico")
                .jsonPath("$.orders[0].items[0].productDescription").isEqualTo("Descripción de Mouse Inalámbrico")
                .jsonPath("$.orders[0].items[0].quantity").isEqualTo(2)
                .jsonPath("$.orders[0].items[0].unitPrice").isEqualTo(35.00)
                .jsonPath("$.orders[0].items[0].subtotal").isEqualTo(70.00)
                // segundo pedido
                .jsonPath("$.orders[1].items[0].productName").isEqualTo("Laptop Pro")
                .jsonPath("$.orders[1].items[0].subtotal").isEqualTo(1500.00);
    }

    @Test
    @DisplayName("debe retornar 200 con lista vacía cuando el usuario no tiene pedidos")
    void getOrdersByUserId_shouldReturn200WithEmptyList_whenUserHasNoOrders() {
        UserData user = persistUser("Usuario Sin Pedidos", uniqueEmail("no-orders"), "Calle Sin Pedidos");

        webTestClient.get()
                .uri("/users/{userId}/orders", user.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(user.getId().intValue())
                .jsonPath("$.totalOrders").isEqualTo(0)
                .jsonPath("$.orders").isEmpty();
    }

    @Test
    @DisplayName("debe retornar 200 con lista vacía cuando el userId no existe en la BD")
    void getOrdersByUserId_shouldReturn200Empty_whenUserIdDoesNotExist() {
        webTestClient.get()
                .uri("/users/999999/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(999999)
                .jsonPath("$.totalOrders").isEqualTo(0)
                .jsonPath("$.orders").isEmpty();
    }

    @Test
    @DisplayName("debe retornar solo los pedidos del usuario consultado (no mezcla con otros usuarios)")
    void getOrdersByUserId_shouldReturnOnlyOrdersForRequestedUser() {
        UserData userA   = persistUser("Usuario A", uniqueEmail("user-a"), "Calle A");
        UserData userB   = persistUser("Usuario B", uniqueEmail("user-b"), "Calle B");
        ProductData product = persistProduct("Auriculares", "Auriculares bluetooth", new BigDecimal("120.00"), 20);

        OrderData orderA = persistOrder(userA.getId(), "PENDING", new BigDecimal("120.00"), false);
        persistOrderItem(orderA.getId(), product.getId(), 1, new BigDecimal("120.00"), new BigDecimal("120.00"));

        OrderData orderB = persistOrder(userB.getId(), "PENDING", new BigDecimal("240.00"), false);
        persistOrderItem(orderB.getId(), product.getId(), 2, new BigDecimal("120.00"), new BigDecimal("240.00"));

        webTestClient.get()
                .uri("/users/{userId}/orders", userA.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(userA.getId().intValue())
                .jsonPath("$.totalOrders").isEqualTo(1)
                .jsonPath("$.orders[0].items[0].productName").isEqualTo("Auriculares");

        webTestClient.get()
                .uri("/users/{userId}/orders", userB.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(userB.getId().intValue())
                .jsonPath("$.totalOrders").isEqualTo(1)
                .jsonPath("$.orders[0].items[0].quantity").isEqualTo(2);
    }

    @Test
    @DisplayName("debe retornar un pedido con múltiples items correctamente agrupados")
    void getOrdersByUserId_shouldGroupMultipleItemsUnderSameOrder() {
        UserData user  = persistUser("Usuario Multi", uniqueEmail("multi-items"), "Calle Multi");
        ProductData p1 = persistProduct("Teclado Mecánico", "Teclado RGB", new BigDecimal("150.00"), 20);
        ProductData p2 = persistProduct("Mouse RGB", "Mouse gaming", new BigDecimal("60.00"), 20);
        ProductData p3 = persistProduct("Pad Mouse XL", "Alfombrilla grande", new BigDecimal("30.00"), 20);

        OrderData order = persistOrder(user.getId(), "COMPLETED", new BigDecimal("240.00"), true);
        persistOrderItem(order.getId(), p1.getId(), 1, new BigDecimal("150.00"), new BigDecimal("150.00"));
        persistOrderItem(order.getId(), p2.getId(), 1, new BigDecimal("60.00"),  new BigDecimal("60.00"));
        persistOrderItem(order.getId(), p3.getId(), 1, new BigDecimal("30.00"),  new BigDecimal("30.00"));

        webTestClient.get()
                .uri("/users/{userId}/orders", user.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalOrders").isEqualTo(1)
                .jsonPath("$.orders[0].orderId").isEqualTo(order.getId().intValue())
                .jsonPath("$.orders[0].items.length()").isEqualTo(3);
    }
}

