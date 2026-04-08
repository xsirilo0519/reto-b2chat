package com.b2chat.order_manager.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RabbitMQConfig Tests")
class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "exchange",        "order.exchange");
        ReflectionTestUtils.setField(config, "processQueue",    "order.process.queue");
        ReflectionTestUtils.setField(config, "processingQueue", "order.processing.queue");
        ReflectionTestUtils.setField(config, "receivedQueue",   "order.notification.received.queue");
        ReflectionTestUtils.setField(config, "completedQueue",  "order.notification.completed.queue");
        ReflectionTestUtils.setField(config, "cancelledQueue",  "order.notification.cancelled.queue");
        ReflectionTestUtils.setField(config, "processKey",      "order.process");
        ReflectionTestUtils.setField(config, "processingKey",   "order.processing");
        ReflectionTestUtils.setField(config, "receivedKey",     "order.notification.received");
        ReflectionTestUtils.setField(config, "completedKey",    "order.notification.completed");
        ReflectionTestUtils.setField(config, "cancelledKey",    "order.notification.cancelled");
    }

    @Test
    @DisplayName("orderExchange - debe crear un TopicExchange con el nombre correcto")
    void orderExchange_shouldHaveCorrectName() {
        TopicExchange exchange = config.orderExchange();
        assertThat(exchange.getName()).isEqualTo("order.exchange");
    }

    @Test
    @DisplayName("orderExchange - debe ser durable")
    void orderExchange_shouldBeDurable() {
        assertThat(config.orderExchange().isDurable()).isTrue();
    }

    @Test
    @DisplayName("orderExchange - no debe ser autoDelete")
    void orderExchange_shouldNotBeAutoDelete() {
        assertThat(config.orderExchange().isAutoDelete()).isFalse();
    }

    @Test
    @DisplayName("orderProcessQueue - debe tener el nombre correcto y ser durable")
    void orderProcessQueue_shouldHaveCorrectNameAndBeDurable() {
        Queue queue = config.orderProcessQueue();
        assertThat(queue.getName()).isEqualTo("order.process.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("orderProcessingQueue - debe tener el nombre correcto y ser durable")
    void orderProcessingQueue_shouldHaveCorrectNameAndBeDurable() {
        Queue queue = config.orderProcessingQueue();
        assertThat(queue.getName()).isEqualTo("order.processing.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("orderReceivedQueue - debe tener el nombre correcto y ser durable")
    void orderReceivedQueue_shouldHaveCorrectNameAndBeDurable() {
        Queue queue = config.orderReceivedQueue();
        assertThat(queue.getName()).isEqualTo("order.notification.received.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("orderCompletedQueue - debe tener el nombre correcto y ser durable")
    void orderCompletedQueue_shouldHaveCorrectNameAndBeDurable() {
        Queue queue = config.orderCompletedQueue();
        assertThat(queue.getName()).isEqualTo("order.notification.completed.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("orderCancelledQueue - debe tener el nombre correcto y ser durable")
    void orderCancelledQueue_shouldHaveCorrectNameAndBeDurable() {
        Queue queue = config.orderCancelledQueue();
        assertThat(queue.getName()).isEqualTo("order.notification.cancelled.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("processBinding - debe enlazar la cola correcta al exchange con la routing key correcta")
    void processBinding_shouldHaveCorrectQueueExchangeAndRoutingKey() {
        TopicExchange exchange = config.orderExchange();
        Queue queue            = config.orderProcessQueue();
        Binding binding        = config.processBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo("order.process.queue");
        assertThat(binding.getExchange()).isEqualTo("order.exchange");
        assertThat(binding.getRoutingKey()).isEqualTo("order.process");
    }

    @Test
    @DisplayName("processingBinding - debe enlazar la cola correcta al exchange con la routing key correcta")
    void processingBinding_shouldHaveCorrectQueueExchangeAndRoutingKey() {
        TopicExchange exchange = config.orderExchange();
        Queue queue            = config.orderProcessingQueue();
        Binding binding        = config.processingBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo("order.processing.queue");
        assertThat(binding.getExchange()).isEqualTo("order.exchange");
        assertThat(binding.getRoutingKey()).isEqualTo("order.processing");
    }

    @Test
    @DisplayName("receivedBinding - debe enlazar la cola correcta al exchange con la routing key correcta")
    void receivedBinding_shouldHaveCorrectQueueExchangeAndRoutingKey() {
        TopicExchange exchange = config.orderExchange();
        Queue queue            = config.orderReceivedQueue();
        Binding binding        = config.receivedBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo("order.notification.received.queue");
        assertThat(binding.getExchange()).isEqualTo("order.exchange");
        assertThat(binding.getRoutingKey()).isEqualTo("order.notification.received");
    }

    @Test
    @DisplayName("completedBinding - debe enlazar la cola correcta al exchange con la routing key correcta")
    void completedBinding_shouldHaveCorrectQueueExchangeAndRoutingKey() {
        TopicExchange exchange = config.orderExchange();
        Queue queue            = config.orderCompletedQueue();
        Binding binding        = config.completedBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo("order.notification.completed.queue");
        assertThat(binding.getExchange()).isEqualTo("order.exchange");
        assertThat(binding.getRoutingKey()).isEqualTo("order.notification.completed");
    }

    @Test
    @DisplayName("cancelledBinding - debe enlazar la cola correcta al exchange con la routing key correcta")
    void cancelledBinding_shouldHaveCorrectQueueExchangeAndRoutingKey() {
        TopicExchange exchange = config.orderExchange();
        Queue queue            = config.orderCancelledQueue();
        Binding binding        = config.cancelledBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo("order.notification.cancelled.queue");
        assertThat(binding.getExchange()).isEqualTo("order.exchange");
        assertThat(binding.getRoutingKey()).isEqualTo("order.notification.cancelled");
    }

    @Test
    @DisplayName("jsonMessageConverter - debe retornar un JacksonJsonMessageConverter")
    void jsonMessageConverter_shouldReturnJacksonConverter() {
        MessageConverter converter = config.jsonMessageConverter();
        assertThat(converter).isNotNull().isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
