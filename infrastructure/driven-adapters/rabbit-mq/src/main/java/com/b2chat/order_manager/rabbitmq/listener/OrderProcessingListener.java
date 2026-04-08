package com.b2chat.order_manager.rabbitmq.listener;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.usecase.OrdersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessingListener {

    private final OrdersUseCase ordersUseCase;

    @RabbitListener(queues = "${rabbitmq.order.process-queue}")
    public void processOrder(OrderEntity orderRequest) {
        log.info("Procesando pedido en segundo plano para userId={}", orderRequest.getUserId());

        ordersUseCase.createOrderUseCase(orderRequest)
                .doOnSuccess(order -> log.info("Pedido procesado exitosamente [id={}]", order.getId()))
                .doOnError(e -> log.error("Error procesando pedido: {}", e.getMessage()))
                .subscribe();
    }
}

