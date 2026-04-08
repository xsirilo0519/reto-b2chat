package com.b2chat.order_manager.rabbitmq.adapter;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderPublishGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class RabbitOrderPublishAdapter implements OrderPublishGateway {

    private final AmqpTemplate amqpTemplate;

    @Value("${rabbitmq.order.exchange}")
    private String exchange;

    @Value("${rabbitmq.order.process-key}")
    private String processKey;

    @Value("${rabbitmq.order.processing-key}")
    private String processingKey;

    @Override
    public Mono<Void> publishOrderForProcessing(OrderEntity order) {
        return Mono.fromRunnable(() ->
                amqpTemplate.convertAndSend(exchange, processKey, order)
        ).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> publishOrderForStockProcessing(OrderEntity order) {
        return Mono.fromRunnable(() ->
                amqpTemplate.convertAndSend(exchange, processingKey, order)
        ).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
