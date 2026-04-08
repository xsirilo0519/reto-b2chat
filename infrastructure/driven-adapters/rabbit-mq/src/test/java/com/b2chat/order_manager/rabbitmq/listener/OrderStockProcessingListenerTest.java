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
@DisplayName("OrderStockProcessingListener Tests")
class OrderStockProcessingListenerTest {

    @Mock
    private OrdersUseCase ordersUseCase;

    private OrderStockProcessingListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderStockProcessingListener(ordersUseCase);
    }

    private OrderEntity buildOrder(OrderStatus status) {
        return OrderEntity.builder()
                .id(1L)
                .userId(1L)
                .status(status)
                .totalAmount(BigDecimal.valueOf(150.0))
                .items(List.of())
                .build();
    }

    @Test
    @DisplayName("processStock - debe invocar processOrderStock con la orden recibida")
    void processStock_shouldCallProcessOrderStock() {
        OrderEntity order = buildOrder(OrderStatus.PROCESSING);
        when(ordersUseCase.processOrderStock(order))
                .thenReturn(Mono.just(buildOrder(OrderStatus.COMPLETED)));

        listener.processStock(order);

        verify(ordersUseCase).processOrderStock(order);
    }

    @Test
    @DisplayName("processStock - debe completar exitosamente cuando el stock es suficiente")
    void processStock_shouldCompleteSuccessfully_whenStockIsSufficient() {
        OrderEntity order = buildOrder(OrderStatus.PROCESSING);
        when(ordersUseCase.processOrderStock(order))
                .thenReturn(Mono.just(buildOrder(OrderStatus.COMPLETED)));

        assertThatNoException().isThrownBy(() -> listener.processStock(order));

        verify(ordersUseCase).processOrderStock(order);
    }

    @Test
    @DisplayName("processStock - no debe lanzar excepción cuando el use case falla (error manejado por doOnError + subscribe)")
    void processStock_shouldNotThrow_whenUseCaseFails() {
        OrderEntity order = buildOrder(OrderStatus.PROCESSING);
        when(ordersUseCase.processOrderStock(order))
                .thenReturn(Mono.error(new RuntimeException("Error de stock")));

        assertThatNoException().isThrownBy(() -> listener.processStock(order));

        verify(ordersUseCase).processOrderStock(order);
    }

    @Test
    @DisplayName("processStock - debe procesar orden con estado COMPLETED correctamente")
    void processStock_shouldProcess_whenOrderIsMarkedCompleted() {
        OrderEntity order = buildOrder(OrderStatus.COMPLETED);
        when(ordersUseCase.processOrderStock(order))
                .thenReturn(Mono.just(buildOrder(OrderStatus.COMPLETED)));

        listener.processStock(order);

        verify(ordersUseCase).processOrderStock(order);
    }

    @Test
    @DisplayName("processStock - debe procesar orden con estado CANCELLED cuando hay stock insuficiente")
    void processStock_shouldProcess_whenOrderIsCancelled() {
        OrderEntity processingOrder = buildOrder(OrderStatus.PROCESSING);
        when(ordersUseCase.processOrderStock(processingOrder))
                .thenReturn(Mono.just(buildOrder(OrderStatus.CANCELLED)));

        listener.processStock(processingOrder);

        verify(ordersUseCase).processOrderStock(processingOrder);
    }
}

