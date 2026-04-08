package com.b2chat.order_manager.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE          = "order.exchange";
    public static final String PROCESS_QUEUE    = "order.process.queue";     // crear orden (PENDING)
    public static final String PROCESSING_QUEUE = "order.processing.queue";  // validar stock (PROCESSING→COMPLETED/CANCELLED)
    public static final String RECEIVED_QUEUE   = "order.received.queue";    // notificación PENDING
    public static final String COMPLETED_QUEUE  = "order.completed.queue";   // notificación COMPLETED
    public static final String CANCELLED_QUEUE  = "order.cancelled.queue";   // notificación CANCELLED
    public static final String PROCESS_KEY      = "order.process";
    public static final String PROCESSING_KEY   = "order.processing";
    public static final String RECEIVED_KEY     = "order.received";
    public static final String COMPLETED_KEY    = "order.completed";
    public static final String CANCELLED_KEY    = "order.cancelled";

    @Bean public TopicExchange orderExchange()  { return new TopicExchange(EXCHANGE, true, false); }

    @Bean public Queue orderProcessQueue()    { return QueueBuilder.durable(PROCESS_QUEUE).build(); }
    @Bean public Queue orderProcessingQueue() { return QueueBuilder.durable(PROCESSING_QUEUE).build(); }
    @Bean public Queue orderReceivedQueue()   { return QueueBuilder.durable(RECEIVED_QUEUE).build(); }
    @Bean public Queue orderCompletedQueue()  { return QueueBuilder.durable(COMPLETED_QUEUE).build(); }
    @Bean public Queue orderCancelledQueue()  { return QueueBuilder.durable(CANCELLED_QUEUE).build(); }

    @Bean public Binding processBinding(@Qualifier("orderProcessQueue")     Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(PROCESS_KEY); }
    @Bean public Binding processingBinding(@Qualifier("orderProcessingQueue") Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(PROCESSING_KEY); }
    @Bean public Binding receivedBinding(@Qualifier("orderReceivedQueue")   Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(RECEIVED_KEY); }
    @Bean public Binding completedBinding(@Qualifier("orderCompletedQueue") Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(COMPLETED_KEY); }
    @Bean public Binding cancelledBinding(@Qualifier("orderCancelledQueue") Queue q, TopicExchange e) { return BindingBuilder.bind(q).to(e).with(CANCELLED_KEY); }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}

