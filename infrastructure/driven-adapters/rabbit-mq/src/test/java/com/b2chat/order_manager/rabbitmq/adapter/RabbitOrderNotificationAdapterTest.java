package com.b2chat.order_manager.rabbitmq.adapter;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitOrderNotificationAdapter Tests")
class RabbitOrderNotificationAdapterTest {

    private static final String EXCHANGE       = "order.exchange";
    private static final String RECEIVED_KEY   = "order.notification.received";
    private static final String COMPLETED_KEY  = "order.notification.completed";
    private static final String CANCELLED_KEY  = "order.notification.cancelled";

    @Mock
    private AmqpTemplate amqpTemplate;

    private RabbitOrderNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RabbitOrderNotificationAdapter(amqpTemplate);
        ReflectionTestUtils.setField(adapter, "exchange",     EXCHANGE);
        ReflectionTestUtils.setField(adapter, "receivedKey",  RECEIVED_KEY);
        ReflectionTestUtils.setField(adapter, "completedKey", COMPLETED_KEY);
        ReflectionTestUtils.setField(adapter, "cancelledKey", CANCELLED_KEY);
    }

    private OrderEntity buildOrder() {
        return OrderEntity.builder()
                .id(10L)
                .userId(2L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(250.0))
                .items(List.of())
                .build();
    }


    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("notifyOrderReceived - debe publicar evento en la routing key de recibido con los campos correctos")
    void notifyOrderReceived_shouldPublishToReceivedKey_withCorrectFields() {
        OrderEntity order = buildOrder();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        StepVerifier.create(adapter.notifyOrderReceived(order, "user@test.com", "Juan"))
                .verifyComplete();

        verify(amqpTemplate).convertAndSend(
                eq(EXCHANGE),
                eq(RECEIVED_KEY),
                captor.capture()
        );

        Map<String, Object> event = captor.getValue();
        assertThat(event)
                .containsEntry("orderId", 10L)
                .containsEntry("userId", 2L)
                .containsEntry("userEmail", "user@test.com")
                .containsEntry("userName", "Juan")
                .containsEntry("status", "PENDING")
                .containsKey("timestamp");
    }

    @Test
    @DisplayName("notifyOrderReceived - debe retornar Mono<Void> que completa sin errores")
    void notifyOrderReceived_shouldReturnEmptyMono() {
        OrderEntity order = buildOrder();

        StepVerifier.create(adapter.notifyOrderReceived(order, "user@test.com", "Juan"))
                .expectNextCount(0)
                .verifyComplete();
    }


    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("notifyOrderCompleted - debe publicar evento en la routing key de completado con los campos correctos")
    void notifyOrderCompleted_shouldPublishToCompletedKey_withCorrectFields() {
        OrderEntity order = buildOrder();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        StepVerifier.create(adapter.notifyOrderCompleted(order, "user@test.com", "María"))
                .verifyComplete();

        verify(amqpTemplate).convertAndSend(
                eq(EXCHANGE),
                eq(COMPLETED_KEY),
                captor.capture()
        );

        Map<String, Object> event = captor.getValue();
        assertThat(event)
                .containsEntry("orderId", 10L)
                .containsEntry("userId", 2L)
                .containsEntry("userEmail", "user@test.com")
                .containsEntry("userName", "María")
                .containsKey("timestamp");
    }


    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("notifyOrderCancelled - debe publicar evento con campo 'reason' en la routing key de cancelado")
    void notifyOrderCancelled_shouldPublishToCancelledKey_withReason() {
        OrderEntity order = buildOrder();
        String reason = "Stock insuficiente para 'Laptop'. Solicitado: 5, disponible: 2";
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        StepVerifier.create(adapter.notifyOrderCancelled(order, "user@test.com", "Carlos", reason))
                .verifyComplete();

        verify(amqpTemplate).convertAndSend(
                eq(EXCHANGE),
                eq(CANCELLED_KEY),
                captor.capture()
        );

        Map<String, Object> event = captor.getValue();
        assertThat(event)
                .containsEntry("orderId", 10L)
                .containsEntry("userEmail", "user@test.com")
                .containsEntry("userName", "Carlos")
                .containsEntry("reason", reason)
                .containsKey("timestamp");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    @DisplayName("notifyOrderReceived - debe propagar error cuando AmqpTemplate lanza excepción")
    void notifyOrderReceived_shouldPropagateError_whenAmqpTemplateFails() {
        OrderEntity order = buildOrder();
        doThrow(new RuntimeException("Broker unavailable"))
                .when(amqpTemplate).convertAndSend(anyString(), anyString(), any(Map.class));

        StepVerifier.create(adapter.notifyOrderReceived(order, "user@test.com", "Juan"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
