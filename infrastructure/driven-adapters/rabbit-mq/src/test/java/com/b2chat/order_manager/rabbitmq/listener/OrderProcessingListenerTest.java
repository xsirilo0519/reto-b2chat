package com.b2chat.order_manager.rabbitmq.listener;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.usecase.OrdersUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderProcessingListener Tests")
class OrderProcessingListenerTest {

    @Mock
    private OrdersUseCase ordersUseCase;

    private OrderProcessingListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderProcessingListener(ordersUseCase);
    }

    private OrderEntity buildOrderRequest() {
        return OrderEntity.builder()
                .id(null)
                .userId(1L)
                .items(List.of())
                .build();
    }

    private OrderEntity buildCreatedOrder() {
        return OrderEntity.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100.0))
                .items(List.of())
                .build();
    }

    @Test
    @DisplayName("processOrder - debe invocar createOrderUseCase con la orden recibida del mensaje")
    void processOrder_shouldCallCreateOrderUseCase() {
        OrderEntity orderRequest = buildOrderRequest();
        when(ordersUseCase.createOrderUseCase(orderRequest))
                .thenReturn(Mono.just(buildCreatedOrder()));

        listener.processOrder(orderRequest);

        verify(ordersUseCase).createOrderUseCase(orderRequest);
    }

    @Test
    @DisplayName("processOrder - debe procesar exitosamente y completar el Mono")
    void processOrder_shouldCompleteSuccessfully() {
        OrderEntity orderRequest = buildOrderRequest();
        OrderEntity created = buildCreatedOrder();
        when(ordersUseCase.createOrderUseCase(orderRequest))
                .thenReturn(Mono.just(created));

        assertThatNoException().isThrownBy(() -> listener.processOrder(orderRequest));

        verify(ordersUseCase).createOrderUseCase(orderRequest);
    }

    @Test
    @DisplayName("processOrder - no debe lanzar excepción cuando el use case falla (error manejado por doOnError + subscribe)")
    void processOrder_shouldNotThrowException_whenUseCaseFails() {
        OrderEntity orderRequest = buildOrderRequest();
        when(ordersUseCase.createOrderUseCase(orderRequest))
                .thenReturn(Mono.error(new RuntimeException("Error de base de datos")));

        assertThatNoException().isThrownBy(() -> listener.processOrder(orderRequest));

        verify(ordersUseCase).createOrderUseCase(orderRequest);
    }

    @Test
    @DisplayName("processOrder - debe delegar la orden con todos sus campos al use case")
    void processOrder_shouldPassCompleteOrderToUseCase() {
        OrderEntity orderRequest = OrderEntity.builder()
                .userId(5L)
                .items(List.of())
                .build();

        when(ordersUseCase.createOrderUseCase(orderRequest))
                .thenReturn(Mono.just(buildCreatedOrder()));

        listener.processOrder(orderRequest);

        verify(ordersUseCase).createOrderUseCase(orderRequest);
    }
}

