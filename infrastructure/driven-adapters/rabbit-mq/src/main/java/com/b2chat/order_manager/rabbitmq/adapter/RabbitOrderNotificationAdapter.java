package com.b2chat.order_manager.rabbitmq.adapter;

import com.b2chat.order_manager.domain.notification.OrderNotificationGateway;
import com.b2chat.order_manager.domain.order.OrderEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitOrderNotificationAdapter implements OrderNotificationGateway {

    private final AmqpTemplate amqpTemplate;

    @Value("${rabbitmq.order.exchange}")
    private String exchange;

    @Value("${rabbitmq.order.received-key}")
    private String receivedKey;

    @Value("${rabbitmq.order.completed-key}")
    private String completedKey;

    @Value("${rabbitmq.order.cancelled-key}")
    private String cancelledKey;

    @Override
    public Mono<Void> notifyOrderReceived(OrderEntity order, String userEmail, String userName) {
        return publish(receivedKey, buildEvent(order, userEmail, userName))
                .doOnSuccess(v -> log.info("Notificación: orden recibida [id={}]", order.getId()));
    }

    @Override
    public Mono<Void> notifyOrderCompleted(OrderEntity order, String userEmail, String userName) {
        return publish(completedKey, buildEvent(order, userEmail, userName))
                .doOnSuccess(v -> log.info("Notificación: orden completada [id={}]", order.getId()));
    }

    @Override
    public Mono<Void> notifyOrderCancelled(OrderEntity order, String userEmail, String userName, String reason) {
        Map<String, Object> event = new java.util.HashMap<>(buildEvent(order, userEmail, userName));
        event.put("reason", reason);
        return publish(cancelledKey, event)
                .doOnSuccess(v -> log.info("Notificación: orden cancelada [id={}] motivo={}", order.getId(), reason));
    }

    private Mono<Void> publish(String routingKey, Map<String, Object> payload) {
        return Mono.fromRunnable(() ->
                amqpTemplate.convertAndSend(exchange, routingKey, payload)
        ).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Map<String, Object> buildEvent(OrderEntity order, String userEmail, String userName) {
        return new java.util.HashMap<>(Map.of(
                "orderId",     order.getId(),
                "userId",      order.getUserId(),
                "userEmail",   userEmail,
                "userName",    userName,
                "status",      order.getStatus().name(),
                "totalAmount", order.getTotalAmount(),
                "timestamp",   LocalDateTime.now().toString()
        ));
    }
}
