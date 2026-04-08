package com.b2chat.order_manager.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.order.exchange}")
    private String exchange;

    @Value("${rabbitmq.order.process-queue}")
    private String processQueue;

    @Value("${rabbitmq.order.processing-queue}")
    private String processingQueue;

    @Value("${rabbitmq.order.received-queue}")
    private String receivedQueue;

    @Value("${rabbitmq.order.completed-queue}")
    private String completedQueue;

    @Value("${rabbitmq.order.cancelled-queue}")
    private String cancelledQueue;

    @Value("${rabbitmq.order.process-key}")
    private String processKey;

    @Value("${rabbitmq.order.processing-key}")
    private String processingKey;

    @Value("${rabbitmq.order.received-key}")
    private String receivedKey;

    @Value("${rabbitmq.order.completed-key}")
    private String completedKey;

    @Value("${rabbitmq.order.cancelled-key}")
    private String cancelledKey;

    @Bean public TopicExchange orderExchange()  { return new TopicExchange(exchange, true, false); }

    @Bean public Queue orderProcessQueue()    { return QueueBuilder.durable(processQueue).build(); }
    @Bean public Queue orderProcessingQueue() { return QueueBuilder.durable(processingQueue).build(); }
    @Bean public Queue orderReceivedQueue()   { return QueueBuilder.durable(receivedQueue).build(); }
    @Bean public Queue orderCompletedQueue()  { return QueueBuilder.durable(completedQueue).build(); }
    @Bean public Queue orderCancelledQueue()  { return QueueBuilder.durable(cancelledQueue).build(); }

    @Bean public Binding processBinding(@Qualifier("orderProcessQueue")       Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(processKey); }
    @Bean public Binding processingBinding(@Qualifier("orderProcessingQueue") Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(processingKey); }
    @Bean public Binding receivedBinding(@Qualifier("orderReceivedQueue")     Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(receivedKey); }
    @Bean public Binding completedBinding(@Qualifier("orderCompletedQueue")   Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(completedKey); }
    @Bean public Binding cancelledBinding(@Qualifier("orderCancelledQueue")   Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(cancelledKey); }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
