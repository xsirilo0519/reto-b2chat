package com.b2chat.order_manager.reactive.web.order;

import com.b2chat.order_manager.domain.exception.InsufficientStockException;
import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.reactive.web.exception.GlobalExceptionHandler;
import com.b2chat.order_manager.reactive.web.order.dto.OrderDto;
import com.b2chat.order_manager.reactive.web.order.dto.OrderItemDto;
import com.b2chat.order_manager.reactive.web.order.dto.UpdateOrderStatusDto;
import com.b2chat.order_manager.usecase.OrdersUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Mock
    private OrdersUseCase ordersUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        OrderController orderController = new OrderController(ordersUseCase);
        webTestClient = WebTestClient.bindToController(orderController)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /orders - debe retornar 202 ACCEPTED cuando la orden es válida")
    void createOrder_shouldReturnAccepted_whenOrderIsValid() {
        OrderDto orderDto = OrderDto.builder()
                .userId(1L)
                .items(List.of(OrderItemDto.builder()
                        .productId(1L)
                        .quantity(2)
                        .build()))
                .build();

        when(ordersUseCase.receiveOrderUseCase(any(OrderEntity.class)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Pedido recibido. Será procesado en breve.");

        verify(ordersUseCase).receiveOrderUseCase(any(OrderEntity.class));
    }

    @Test
    @DisplayName("POST /orders - debe retornar 400 BAD REQUEST cuando userId es nulo")
    void createOrder_shouldReturnBadRequest_whenUserIdIsNull() {
        OrderDto orderDto = OrderDto.builder()
                .userId(null)
                .items(List.of(OrderItemDto.builder().productId(1L).quantity(1).build()))
                .build();

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /orders - debe retornar 400 BAD REQUEST cuando la lista de items está vacía")
    void createOrder_shouldReturnBadRequest_whenItemsListIsEmpty() {
        OrderDto orderDto = OrderDto.builder()
                .userId(1L)
                .items(List.of())
                .build();

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /orders - debe retornar 400 BAD REQUEST cuando quantity del item es menor a 1")
    void createOrder_shouldReturnBadRequest_whenItemQuantityIsZero() {
        OrderDto orderDto = OrderDto.builder()
                .userId(1L)
                .items(List.of(OrderItemDto.builder()
                        .productId(1L)
                        .quantity(0)
                        .build()))
                .build();

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isBadRequest();
    }


    @Test
    @DisplayName("GET /orders/{id} - debe retornar 200 OK con la orden cuando existe")
    void getOrderById_shouldReturnOrder_whenFound() {
        OrderEntity orderEntity = OrderEntity.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100.0))
                .items(List.of(OrderItemEntity.builder()
                        .id(1L)
                        .orderId(1L)
                        .productId(1L)
                        .quantity(2)
                        .unitPrice(BigDecimal.valueOf(50.0))
                        .total(BigDecimal.valueOf(100.0))
                        .build()))
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();

        when(ordersUseCase.getOrderByIdUseCase(1L)).thenReturn(Mono.just(orderEntity));

        webTestClient.get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.userId").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("PENDING")
                .jsonPath("$.totalAmount").isEqualTo(100.0)
                .jsonPath("$.items[0].productId").isEqualTo(1)
                .jsonPath("$.items[0].quantity").isEqualTo(2);

        verify(ordersUseCase).getOrderByIdUseCase(1L);
    }


    @Test
    @DisplayName("PUT /orders/{id}/status - debe retornar 200 OK con la orden actualizada")
    void updateOrderStatus_shouldReturnUpdatedOrder_whenValid() {
        OrderEntity updatedOrder = OrderEntity.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.PROCESSING)
                .totalAmount(BigDecimal.valueOf(100.0))
                .items(List.of())
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 11, 0))
                .build();

        UpdateOrderStatusDto dto = UpdateOrderStatusDto.builder()
                .status(OrderStatus.PROCESSING)
                .build();

        when(ordersUseCase.updateOrderStatusUseCase(eq(1L), eq(OrderStatus.PROCESSING)))
                .thenReturn(Mono.just(updatedOrder));

        webTestClient.put()
                .uri("/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("PROCESSING");

        verify(ordersUseCase).updateOrderStatusUseCase(1L, OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("PUT /orders/{id}/status - debe retornar 400 BAD REQUEST cuando el estado es nulo")
    void updateOrderStatus_shouldReturnBadRequest_whenStatusIsNull() {
        UpdateOrderStatusDto dto = UpdateOrderStatusDto.builder()
                .status(null)
                .build();

        webTestClient.put()
                .uri("/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("PUT /orders/{id}/status - debe actualizar a COMPLETED correctamente")
    void updateOrderStatus_shouldReturnOk_whenStatusIsCompleted() {
        OrderEntity completedOrder = OrderEntity.builder()
                .id(2L)
                .userId(3L)
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(200.0))
                .items(List.of())
                .createdAt(LocalDateTime.of(2025, 2, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2025, 2, 1, 10, 0))
                .build();

        UpdateOrderStatusDto dto = UpdateOrderStatusDto.builder()
                .status(OrderStatus.COMPLETED)
                .build();

        when(ordersUseCase.updateOrderStatusUseCase(eq(2L), eq(OrderStatus.COMPLETED)))
                .thenReturn(Mono.just(completedOrder));

        webTestClient.put()
                .uri("/orders/2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(2)
                .jsonPath("$.status").isEqualTo("COMPLETED");
    }


    @Test
    @DisplayName("GET /orders/{id} - debe retornar 404 cuando la orden no existe (ResourceNotFoundException)")
    void getOrderById_shouldReturn404_whenOrderNotFound() {
        when(ordersUseCase.getOrderByIdUseCase(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Order", 99L)));

        webTestClient.get()
                .uri("/orders/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Order no encontrado con id: 99");
    }

    @Test
    @DisplayName("POST /orders - debe retornar 404 cuando el usuario no existe (ResourceNotFoundException)")
    void createOrder_shouldReturn404_whenUserNotFound() {
        OrderDto orderDto = OrderDto.builder()
                .userId(99L)
                .items(List.of(OrderItemDto.builder().productId(1L).quantity(1).build()))
                .build();

        when(ordersUseCase.receiveOrderUseCase(any(OrderEntity.class)))
                .thenReturn(Mono.error(new ResourceNotFoundException("User", 99L)));

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(orderDto)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("User no encontrado con id: 99");
    }

    @Test
    @DisplayName("PUT /orders/{id}/status - debe retornar 422 cuando hay stock insuficiente (InsufficientStockException)")
    void updateOrderStatus_shouldReturn422_whenInsufficientStock() {
        UpdateOrderStatusDto dto = UpdateOrderStatusDto.builder()
                .status(OrderStatus.PROCESSING)
                .build();

        when(ordersUseCase.updateOrderStatusUseCase(eq(1L), eq(OrderStatus.PROCESSING)))
                .thenReturn(Mono.error(new InsufficientStockException("Laptop", 5, 2)));

        webTestClient.put()
                .uri("/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(422)
                .jsonPath("$.message").isEqualTo(
                        "Stock insuficiente para 'Laptop'. Solicitado: 5, disponible: 2");
    }

    @Test
    @DisplayName("PUT /orders/{id}/status - debe retornar 400 con mensaje de valores válidos cuando el enum es inválido (ServerWebInputException)")
    void updateOrderStatus_shouldReturn400_whenInvalidEnumValue() {
        Map<String, String> invalidBody = Map.of("status", "INVALID_STATUS");

        webTestClient.put()
                .uri("/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(msg ->
                        org.assertj.core.api.Assertions.assertThat(msg.toString())
                                .contains("INVALID_STATUS")
                                .contains("PENDING")
                                .contains("PROCESSING")
                                .contains("COMPLETED")
                                .contains("CANCELLED"));
    }

    @Test
    @DisplayName("POST /orders - debe retornar 400 con 'Formato de entrada inválido' cuando el tipo del campo es incorrecto (else de ServerWebInputException)")
    void createOrder_shouldReturn400WithGenericMessage_whenFieldTypeIsInvalid() {

        Map<String, Object> invalidBody = Map.of(
                "userId", "not-a-number",
                "items", List.of(Map.of("productId", 1, "quantity", 1))
        );

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Formato de entrada inválido");
    }
}

