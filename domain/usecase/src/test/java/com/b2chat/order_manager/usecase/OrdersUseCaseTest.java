package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.exception.InsufficientStockException;
import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.notification.OrderNotificationGateway;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderGateway;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderPublishGateway;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.domain.users.gateway.UserGateway;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdersUseCase Tests")
class OrdersUseCaseTest {

    @Mock private OrderGateway orderGateway;
    @Mock private ProductGateway productGateway;
    @Mock private UserGateway userGateway;
    @Mock private OrderNotificationGateway notificationGateway;
    @Mock private OrderPublishGateway publishGateway;

    private OrdersUseCase ordersUseCase;

    @BeforeEach
    void setUp() {
        ordersUseCase = new OrdersUseCase(
                orderGateway, productGateway, userGateway, notificationGateway, publishGateway);
    }


    private UserEntity buildUser() {
        return UserEntity.builder()
                .id(1L).name("Juan Pérez").email("juan@test.com").address("Calle 1").build();
    }

    private ProductEntity buildProduct(Long id, BigDecimal price, int stock) {
        return ProductEntity.builder()
                .id(id).name("Producto " + id).price(price).stockQuantity(stock).build();
    }

    private OrderEntity buildOrderRequest() {
        return OrderEntity.builder()
                .userId(1L)
                .items(List.of(
                        OrderItemEntity.builder().productId(10L).quantity(2).build()
                ))
                .build();
    }

    private OrderEntity buildSavedOrder(OrderStatus status) {
        return OrderEntity.builder()
                .id(1L).userId(1L).status(status)
                .totalAmount(BigDecimal.valueOf(100.0))
                .items(List.of(OrderItemEntity.builder()
                        .id(1L).productId(10L).quantity(2)
                        .unitPrice(BigDecimal.valueOf(50.0))
                        .total(BigDecimal.valueOf(100.0))
                        .build()))
                .build();
    }


    @Test
    @DisplayName("receiveOrderUseCase - debe validar usuario y productos, luego publicar la orden")
    void receiveOrderUseCase_shouldValidateAndPublish_whenOrderIsValid() {
        OrderEntity orderRequest = buildOrderRequest();
        when(userGateway.getUserById(1L)).thenReturn(Mono.just(buildUser()));
        when(productGateway.findProductById(10L)).thenReturn(Mono.just(buildProduct(10L, BigDecimal.valueOf(50.0), 5)));
        when(publishGateway.publishOrderForProcessing(orderRequest)).thenReturn(Mono.empty());

        StepVerifier.create(ordersUseCase.receiveOrderUseCase(orderRequest))
                .verifyComplete();

        verify(userGateway).getUserById(1L);
        verify(productGateway).findProductById(10L);
        verify(publishGateway).publishOrderForProcessing(orderRequest);
    }

    @Test
    @DisplayName("receiveOrderUseCase - debe fallar cuando el usuario no existe")
    void receiveOrderUseCase_shouldFail_whenUserNotFound() {
        OrderEntity orderRequest = buildOrderRequest();
        when(userGateway.getUserById(1L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Usuario", 1L)));

        StepVerifier.create(ordersUseCase.receiveOrderUseCase(orderRequest))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(publishGateway, never()).publishOrderForProcessing(any());
    }

    @Test
    @DisplayName("receiveOrderUseCase - debe fallar cuando un producto no existe")
    void receiveOrderUseCase_shouldFail_whenProductNotFound() {
        OrderEntity orderRequest = buildOrderRequest();
        when(userGateway.getUserById(1L)).thenReturn(Mono.just(buildUser()));
        when(productGateway.findProductById(10L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Producto", 10L)));

        StepVerifier.create(ordersUseCase.receiveOrderUseCase(orderRequest))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }


    @Test
    @DisplayName("createOrderUseCase - debe enriquecer items con precios, calcular total y guardar la orden")
    void createOrderUseCase_shouldEnrichItemsAndCalculateTotal() {
        OrderEntity orderRequest = buildOrderRequest();
        ProductEntity product = buildProduct(10L, BigDecimal.valueOf(50.0), 10);
        OrderEntity savedOrder = buildSavedOrder(OrderStatus.PENDING);

        when(userGateway.getUserById(1L)).thenReturn(Mono.just(buildUser()));
        when(productGateway.findProductById(10L)).thenReturn(Mono.just(product));
        when(orderGateway.createOrder(any(OrderEntity.class))).thenReturn(Mono.just(savedOrder));
        when(notificationGateway.notifyOrderReceived(any(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(ordersUseCase.createOrderUseCase(orderRequest))
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(1L);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.0));
                })
                .verifyComplete();

        verify(orderGateway).createOrder(any(OrderEntity.class));
        verify(notificationGateway).notifyOrderReceived(any(), eq("juan@test.com"), eq("Juan Pérez"));
    }

    @Test
    @DisplayName("createOrderUseCase - debe guardar la orden con estado PENDING")
    void createOrderUseCase_shouldCreateOrderWithPendingStatus() {
        OrderEntity orderRequest = buildOrderRequest();
        when(userGateway.getUserById(1L)).thenReturn(Mono.just(buildUser()));
        when(productGateway.findProductById(10L))
                .thenReturn(Mono.just(buildProduct(10L, BigDecimal.valueOf(30.0), 5)));
        when(orderGateway.createOrder(any(OrderEntity.class)))
                .thenReturn(Mono.just(buildSavedOrder(OrderStatus.PENDING)));
        when(notificationGateway.notifyOrderReceived(any(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(ordersUseCase.createOrderUseCase(orderRequest))
                .assertNext(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING))
                .verifyComplete();
    }

    @Test
    @DisplayName("createOrderUseCase - debe fallar cuando el usuario no existe")
    void createOrderUseCase_shouldFail_whenUserNotFound() {
        when(userGateway.getUserById(1L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Usuario", 1L)));

        StepVerifier.create(ordersUseCase.createOrderUseCase(buildOrderRequest()))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(orderGateway, never()).createOrder(any());
    }


    @Test
    @DisplayName("updateOrderStatusUseCase - status PROCESSING debe actualizar y publicar a la cola de stock")
    void updateOrderStatusUseCase_shouldUpdateAndPublishToStockQueue_whenProcessing() {
        OrderEntity processingOrder = buildSavedOrder(OrderStatus.PROCESSING);
        when(orderGateway.updateOrderStatus(1L, OrderStatus.PROCESSING))
                .thenReturn(Mono.just(processingOrder));
        when(publishGateway.publishOrderForStockProcessing(processingOrder))
                .thenReturn(Mono.empty());

        StepVerifier.create(ordersUseCase.updateOrderStatusUseCase(1L, OrderStatus.PROCESSING))
                .assertNext(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING))
                .verifyComplete();

        verify(publishGateway).publishOrderForStockProcessing(processingOrder);
    }

    @Test
    @DisplayName("updateOrderStatusUseCase - status CANCELLED solo debe actualizar sin publicar")
    void updateOrderStatusUseCase_shouldOnlyUpdate_whenStatusIsNotProcessing() {
        OrderEntity cancelledOrder = buildSavedOrder(OrderStatus.CANCELLED);
        when(orderGateway.updateOrderStatus(1L, OrderStatus.CANCELLED))
                .thenReturn(Mono.just(cancelledOrder));

        StepVerifier.create(ordersUseCase.updateOrderStatusUseCase(1L, OrderStatus.CANCELLED))
                .assertNext(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED))
                .verifyComplete();

        verify(publishGateway, never()).publishOrderForStockProcessing(any());
    }

    @Test
    @DisplayName("updateOrderStatusUseCase - status COMPLETED solo debe actualizar sin publicar")
    void updateOrderStatusUseCase_shouldOnlyUpdate_whenStatusIsCompleted() {
        OrderEntity completedOrder = buildSavedOrder(OrderStatus.COMPLETED);
        when(orderGateway.updateOrderStatus(1L, OrderStatus.COMPLETED))
                .thenReturn(Mono.just(completedOrder));

        StepVerifier.create(ordersUseCase.updateOrderStatusUseCase(1L, OrderStatus.COMPLETED))
                .assertNext(order -> assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED))
                .verifyComplete();

        verify(publishGateway, never()).publishOrderForStockProcessing(any());
    }


    @Test
    @DisplayName("processOrderStock - debe marcar COMPLETED directamente cuando order.isCompleted() es true")
    void processOrderStock_shouldMarkCompleted_whenOrderIsAlreadyFlaggedCompleted() {
        OrderEntity order = OrderEntity.builder()
                .id(1L).userId(1L).completed(true).status(OrderStatus.PROCESSING)
                .items(List.of()).build();
        OrderEntity completedOrder = order.toBuilder().status(OrderStatus.COMPLETED).build();
        UserEntity user = buildUser();

        when(orderGateway.updateOrderStatus(1L, OrderStatus.COMPLETED))
                .thenReturn(Mono.just(completedOrder));
        when(userGateway.getUserById(1L)).thenReturn(Mono.just(user));
        when(notificationGateway.notifyOrderCompleted(eq(completedOrder), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(ordersUseCase.processOrderStock(order))
                .assertNext(result -> assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED))
                .verifyComplete();

        verify(notificationGateway).notifyOrderCompleted(eq(completedOrder), eq("juan@test.com"), eq("Juan Pérez"));
    }

    @Test
    @DisplayName("processOrderStock - debe decrementar stock y marcar COMPLETED cuando el stock es suficiente")
    void processOrderStock_shouldDecrementStockAndComplete_whenStockIsSufficient() {
        OrderEntity order = OrderEntity.builder()
                .id(1L).userId(1L).completed(false).status(OrderStatus.PROCESSING)
                .items(List.of(OrderItemEntity.builder().productId(10L).quantity(2).build()))
                .build();
        ProductEntity product = buildProduct(10L, BigDecimal.valueOf(50.0), 10);
        OrderEntity completedOrder = order.toBuilder().status(OrderStatus.COMPLETED).build();
        UserEntity user = buildUser();

        when(userGateway.getUserById(1L)).thenReturn(Mono.just(user));
        when(productGateway.findProductById(10L)).thenReturn(Mono.just(product));
        when(productGateway.decrementStock(10L, 2)).thenReturn(Mono.empty());
        when(orderGateway.updateOrderStatus(1L, OrderStatus.COMPLETED))
                .thenReturn(Mono.just(completedOrder));
        when(notificationGateway.notifyOrderCompleted(eq(completedOrder), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(ordersUseCase.processOrderStock(order))
                .assertNext(result -> assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED))
                .verifyComplete();

        verify(productGateway).decrementStock(10L, 2);
        verify(orderGateway).updateOrderStatus(1L, OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("processOrderStock - debe cancelar la orden y notificar cuando el stock es insuficiente")
    void processOrderStock_shouldCancelOrderAndNotify_whenStockIsInsufficient() {
        OrderEntity order = OrderEntity.builder()
                .id(1L).userId(1L).completed(false).status(OrderStatus.PROCESSING)
                .items(List.of(OrderItemEntity.builder().productId(10L).quantity(5).build()))
                .build();
        ProductEntity product = buildProduct(10L, BigDecimal.valueOf(50.0), 2);
        OrderEntity cancelledOrder = order.toBuilder().status(OrderStatus.CANCELLED).build();
        UserEntity user = buildUser();

        when(userGateway.getUserById(1L)).thenReturn(Mono.just(user));
        when(productGateway.findProductById(10L)).thenReturn(Mono.just(product));
        when(orderGateway.updateOrderStatus(1L, OrderStatus.CANCELLED))
                .thenReturn(Mono.just(cancelledOrder));
        when(notificationGateway.notifyOrderCancelled(eq(cancelledOrder), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(ordersUseCase.processOrderStock(order))
                .assertNext(result -> assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED))
                .verifyComplete();

        verify(productGateway, never()).decrementStock(any(), any());
        verify(orderGateway).updateOrderStatus(1L, OrderStatus.CANCELLED);
        verify(notificationGateway).notifyOrderCancelled(
                eq(cancelledOrder), eq("juan@test.com"), eq("Juan Pérez"), anyString());
    }


    @Test
    @DisplayName("getOrderByIdUseCase - debe delegar al gateway y retornar la orden")
    void getOrderByIdUseCase_shouldReturnOrder_fromGateway() {
        OrderEntity order = buildSavedOrder(OrderStatus.PENDING);
        when(orderGateway.getOrderById(1L)).thenReturn(Mono.just(order));

        StepVerifier.create(ordersUseCase.getOrderByIdUseCase(1L))
                .assertNext(result -> assertThat(result.getId()).isEqualTo(1L))
                .verifyComplete();

        verify(orderGateway).getOrderById(1L);
    }

    @Test
    @DisplayName("getOrderByIdUseCase - debe propagar el error del gateway cuando no existe")
    void getOrderByIdUseCase_shouldPropagateError_whenGatewayFails() {
        when(orderGateway.getOrderById(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Orden", 99L)));

        StepVerifier.create(ordersUseCase.getOrderByIdUseCase(99L))
                .expectErrorMatches(ex -> ex instanceof ResourceNotFoundException
                        && ex.getMessage().contains("99"))
                .verify();
    }


    @Test
    @DisplayName("validateOrderRequest - debe completar cuando usuario y productos existen")
    void validateOrderRequest_shouldComplete_whenUserAndProductsExist() {
        OrderEntity orderRequest = buildOrderRequest();
        when(userGateway.getUserById(1L)).thenReturn(Mono.just(buildUser()));
        when(productGateway.findProductById(10L))
                .thenReturn(Mono.just(buildProduct(10L, BigDecimal.valueOf(50.0), 5)));

        StepVerifier.create(ordersUseCase.validateOrderRequest(orderRequest))
                .verifyComplete();
    }

    @Test
    @DisplayName("validateOrderRequest - debe fallar cuando el usuario no existe")
    void validateOrderRequest_shouldFail_whenUserNotFound() {
        OrderEntity orderRequest = buildOrderRequest();
        when(userGateway.getUserById(1L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Usuario", 1L)));

        StepVerifier.create(ordersUseCase.validateOrderRequest(orderRequest))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("validateOrderRequest - debe fallar cuando un producto del pedido no existe")
    void validateOrderRequest_shouldFail_whenProductNotFound() {
        OrderEntity orderRequest = buildOrderRequest();
        when(userGateway.getUserById(1L)).thenReturn(Mono.just(buildUser()));
        when(productGateway.findProductById(10L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Producto", 10L)));

        StepVerifier.create(ordersUseCase.validateOrderRequest(orderRequest))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }
}

