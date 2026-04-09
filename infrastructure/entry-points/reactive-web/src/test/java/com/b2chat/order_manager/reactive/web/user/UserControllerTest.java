package com.b2chat.order_manager.reactive.web.user;

import com.b2chat.order_manager.domain.exception.DuplicateEmailException;
import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.reactive.web.exception.GlobalExceptionHandler;
import com.b2chat.order_manager.reactive.web.user.dto.UserDto;
import com.b2chat.order_manager.usecase.OrdersUseCase;
import com.b2chat.order_manager.usecase.UsersUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock private UsersUseCase usersUseCase;
    @Mock private OrdersUseCase ordersUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(usersUseCase, ordersUseCase);
        webTestClient = WebTestClient.bindToController(userController)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserEntity buildUser(Long id, String name, String email, String address) {
        return UserEntity.builder().id(id).name(name).email(email).address(address)
                .createdAt(LocalDateTime.of(2025, 1, 10, 8, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 10, 8, 0))
                .build();
    }

    private OrderEntity buildOrderWithItems(Long orderId, OrderStatus status,
                                            BigDecimal total, String productName) {
        return OrderEntity.builder()
                .id(orderId).userId(1L).status(status).totalAmount(total)
                .createdAt(LocalDateTime.of(2026, 4, 8, 10, 0))
                .items(List.of(OrderItemEntity.builder()
                        .id(1L).productId(10L)
                        .productName(productName).productDescription("Desc " + productName)
                        .quantity(1).unitPrice(total).total(total)
                        .build()))
                .build();
    }

    // ── GET /users/{id} ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /users/{id} - debe retornar 200 OK con el usuario cuando existe")
    void getUserById_shouldReturnUser_whenFound() {
        when(usersUseCase.getUserByIdUseCase(1L))
                .thenReturn(Mono.just(buildUser(1L, "Juan Pérez", "juan.perez@example.com", "Calle 123, Bogotá")));

        webTestClient.get().uri("/users/1").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Juan Pérez")
                .jsonPath("$.email").isEqualTo("juan.perez@example.com")
                .jsonPath("$.address").isEqualTo("Calle 123, Bogotá");

        verify(usersUseCase).getUserByIdUseCase(1L);
    }

    @Test
    @DisplayName("GET /users/{id} - debe verificar todos los campos del response")
    void getUserById_shouldReturnAllFields_whenFound() {
        when(usersUseCase.getUserByIdUseCase(5L))
                .thenReturn(Mono.just(buildUser(5L, "María López", "maria.lopez@example.com", "Av. Siempreviva 742")));

        webTestClient.get().uri("/users/5").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.name").isEqualTo("María López")
                .jsonPath("$.email").isEqualTo("maria.lopez@example.com")
                .jsonPath("$.address").isEqualTo("Av. Siempreviva 742");

        verify(usersUseCase).getUserByIdUseCase(5L);
    }

    @Test
    @DisplayName("GET /users/{id} - debe retornar 404 cuando el usuario no existe")
    void getUserById_shouldReturn404_whenUserNotFound() {
        when(usersUseCase.getUserByIdUseCase(5L))
                .thenReturn(Mono.error(new ResourceNotFoundException("User", 5L)));

        webTestClient.get().uri("/users/5").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("User no encontrado con id: 5");
    }

    // ── POST /users ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /users - debe retornar 201 CREATED cuando el usuario es válido")
    void createUser_shouldReturnCreated_whenUserIsValid() {
        UserDto dto = UserDto.builder()
                .name("Carlos Ruiz").email("carlos.ruiz@example.com").address("Carrera 45, Medellín").build();
        when(usersUseCase.createUserUseCase(any(UserEntity.class)))
                .thenReturn(Mono.just(buildUser(1L, "Carlos Ruiz", "carlos.ruiz@example.com", "Carrera 45, Medellín")));

        webTestClient.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto).exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Carlos Ruiz");

        verify(usersUseCase).createUserUseCase(any(UserEntity.class));
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 cuando el nombre está vacío")
    void createUser_shouldReturnBadRequest_whenNameIsBlank() {
        UserDto dto = UserDto.builder().name("").email("valido@example.com").address("Calle 10").build();
        webTestClient.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto).exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 cuando el nombre contiene números")
    void createUser_shouldReturnBadRequest_whenNameContainsNumbers() {
        UserDto dto = UserDto.builder().name("Carlos123").email("carlos@example.com").address("Calle 10").build();
        webTestClient.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto).exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 cuando el email tiene formato inválido")
    void createUser_shouldReturnBadRequest_whenEmailIsInvalid() {
        UserDto dto = UserDto.builder().name("Ana Torres").email("no-es-un-email").address("Calle 10").build();
        webTestClient.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto).exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 cuando el email está vacío")
    void createUser_shouldReturnBadRequest_whenEmailIsBlank() {
        UserDto dto = UserDto.builder().name("Ana Torres").email("").address("Calle 10").build();
        webTestClient.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto).exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 cuando la dirección está vacía")
    void createUser_shouldReturnBadRequest_whenAddressIsBlank() {
        UserDto dto = UserDto.builder().name("Ana Torres").email("ana@example.com").address("").build();
        webTestClient.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto).exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 409 cuando el email ya existe")
    void createUser_shouldReturn409_whenEmailAlreadyExists() {
        UserDto dto = UserDto.builder()
                .name("Carlos Ruiz").email("carlos.ruiz@example.com").address("Carrera 45, Medellín").build();
        when(usersUseCase.createUserUseCase(any(UserEntity.class)))
                .thenReturn(Mono.error(new DuplicateEmailException("carlos.ruiz@example.com")));

        webTestClient.post().uri("/users").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.message").isEqualTo("El usuario ya está registrado: carlos.ruiz@example.com");
    }

    // ── GET /users/{userId}/orders ────────────────────────────────────────────

    @Test
    @DisplayName("GET /users/{userId}/orders - debe retornar 200 con pedidos y detalles de productos")
    void getOrdersByUserId_shouldReturn200WithOrdersAndProductDetails() {
        OrderEntity order = buildOrderWithItems(5L, OrderStatus.COMPLETED,
                new BigDecimal("1200.00"), "Laptop Gaming");
        when(ordersUseCase.getOrdersByUserIdUseCase(1L)).thenReturn(Flux.just(order));

        webTestClient.get().uri("/users/1/orders").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(1)
                .jsonPath("$.totalOrders").isEqualTo(1)
                .jsonPath("$.orders[0].orderId").isEqualTo(5)
                .jsonPath("$.orders[0].status").isEqualTo("COMPLETED")
                .jsonPath("$.orders[0].totalAmount").isEqualTo(1200.00)
                .jsonPath("$.orders[0].items[0].productName").isEqualTo("Laptop Gaming")
                .jsonPath("$.orders[0].items[0].productDescription").isEqualTo("Desc Laptop Gaming")
                .jsonPath("$.orders[0].items[0].quantity").isEqualTo(1)
                .jsonPath("$.orders[0].items[0].subtotal").isEqualTo(1200.00);

        verify(ordersUseCase).getOrdersByUserIdUseCase(1L);
    }

    @Test
    @DisplayName("GET /users/{userId}/orders - debe retornar 200 con lista vacía cuando no hay pedidos")
    void getOrdersByUserId_shouldReturn200WithEmptyList_whenNoOrders() {
        when(ordersUseCase.getOrdersByUserIdUseCase(1L)).thenReturn(Flux.empty());

        webTestClient.get().uri("/users/1/orders").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(1)
                .jsonPath("$.totalOrders").isEqualTo(0)
                .jsonPath("$.orders").isEmpty();

        verify(ordersUseCase).getOrdersByUserIdUseCase(1L);
    }

    @Test
    @DisplayName("GET /users/{userId}/orders - totalOrders debe coincidir con el tamaño de la lista")
    void getOrdersByUserId_totalOrdersShouldMatchListSize() {
        when(ordersUseCase.getOrdersByUserIdUseCase(1L)).thenReturn(Flux.just(
                buildOrderWithItems(1L, OrderStatus.PENDING,   new BigDecimal("500.00"), "Producto A"),
                buildOrderWithItems(2L, OrderStatus.COMPLETED, new BigDecimal("800.00"), "Producto B"),
                buildOrderWithItems(3L, OrderStatus.CANCELLED, new BigDecimal("200.00"), "Producto C")
        ));

        webTestClient.get().uri("/users/1/orders").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalOrders").isEqualTo(3)
                .jsonPath("$.orders.length()").isEqualTo(3);
    }

    @Test
    @DisplayName("GET /users/{userId}/orders - debe retornar 200 vacío aunque el userId no exista (sin 404)")
    void getOrdersByUserId_shouldReturn200Empty_evenWhenUserDoesNotExist() {
        when(ordersUseCase.getOrdersByUserIdUseCase(999L)).thenReturn(Flux.empty());

        webTestClient.get().uri("/users/999/orders").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(999)
                .jsonPath("$.totalOrders").isEqualTo(0);
    }
}
