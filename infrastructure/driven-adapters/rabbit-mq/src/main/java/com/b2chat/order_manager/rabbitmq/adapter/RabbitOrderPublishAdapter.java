package com.b2chat.order_manager.rabbitmq.adapter;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderPublishGateway;
import com.b2chat.order_manager.rabbitmq.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class RabbitOrderPublishAdapter implements OrderPublishGateway {

    private final AmqpTemplate amqpTemplate;

    @Override
    public Mono<Void> publishOrderForProcessing(OrderEntity order) {
        return Mono.fromRunnable(() ->
                amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PROCESS_KEY, order)
        ).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> publishOrderForStockProcessing(OrderEntity order) {
        return Mono.fromRunnable(() ->
                amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PROCESSING_KEY, order)
        ).subscribeOn(Schedulers.boundedElastic()).then();
    }
}

