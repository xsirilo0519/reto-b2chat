package com.b2chat.order_manager.rabbitmq.adapter;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitOrderPublishAdapter Tests")
class RabbitOrderPublishAdapterTest {

    private static final String EXCHANGE       = "order.exchange";
    private static final String PROCESS_KEY    = "order.process";
    private static final String PROCESSING_KEY = "order.processing";

    @Mock
    private AmqpTemplate amqpTemplate;

    private RabbitOrderPublishAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RabbitOrderPublishAdapter(amqpTemplate);
        ReflectionTestUtils.setField(adapter, "exchange",      EXCHANGE);
        ReflectionTestUtils.setField(adapter, "processKey",    PROCESS_KEY);
        ReflectionTestUtils.setField(adapter, "processingKey", PROCESSING_KEY);
    }

    private OrderEntity buildOrder() {
        return OrderEntity.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100.0))
                .items(List.of())
                .build();
    }


    @Test
    @DisplayName("publishOrderForProcessing - debe publicar la orden en el exchange con la routing key correcta")
    void publishOrderForProcessing_shouldSendToProcessKey() {
        OrderEntity order = buildOrder();

        StepVerifier.create(adapter.publishOrderForProcessing(order))
                .verifyComplete();

        verify(amqpTemplate).convertAndSend(
                EXCHANGE,
                PROCESS_KEY,
                order
        );
    }

    @Test
    @DisplayName("publishOrderForProcessing - debe retornar Mono<Void> que completa sin emitir elementos")
    void publishOrderForProcessing_shouldReturnEmptyMono() {
        OrderEntity order = buildOrder();

        StepVerifier.create(adapter.publishOrderForProcessing(order))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("publishOrderForProcessing - debe propagar error cuando AmqpTemplate lanza excepción")
    void publishOrderForProcessing_shouldPropagateError_whenAmqpTemplateFails() {
        OrderEntity order = buildOrder();
        doThrow(new RuntimeException("RabbitMQ connection error"))
                .when(amqpTemplate).convertAndSend(anyString(), anyString(), any(OrderEntity.class));

        StepVerifier.create(adapter.publishOrderForProcessing(order))
                .expectError(RuntimeException.class)
                .verify();
    }


    @Test
    @DisplayName("publishOrderForStockProcessing - debe publicar la orden con la routing key de stock")
    void publishOrderForStockProcessing_shouldSendToProcessingKey() {
        OrderEntity order = buildOrder();

        StepVerifier.create(adapter.publishOrderForStockProcessing(order))
                .verifyComplete();

        verify(amqpTemplate).convertAndSend(
                EXCHANGE,
                PROCESSING_KEY,
                order
        );
    }

    @Test
    @DisplayName("publishOrderForStockProcessing - debe retornar Mono<Void> que completa sin emitir elementos")
    void publishOrderForStockProcessing_shouldReturnEmptyMono() {
        OrderEntity order = buildOrder();

        StepVerifier.create(adapter.publishOrderForStockProcessing(order))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("publishOrderForStockProcessing - debe propagar error cuando AmqpTemplate lanza excepción")
    void publishOrderForStockProcessing_shouldPropagateError_whenAmqpTemplateFails() {
        OrderEntity order = buildOrder();
        doThrow(new RuntimeException("RabbitMQ broker unavailable"))
                .when(amqpTemplate).convertAndSend(anyString(), anyString(), any(OrderEntity.class));

        StepVerifier.create(adapter.publishOrderForStockProcessing(order))
                .expectError(RuntimeException.class)
                .verify();
    }
}
