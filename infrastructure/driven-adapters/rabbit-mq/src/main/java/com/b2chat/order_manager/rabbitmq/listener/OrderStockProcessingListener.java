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
public class OrderStockProcessingListener {

    private final OrdersUseCase ordersUseCase;

    @RabbitListener(queues = "${rabbitmq.order.processing-queue}")
    public void processStock(OrderEntity order) {
        log.info("Procesando stock para orden [id={}]", order.getId());

        ordersUseCase.processOrderStock(order)
                .doOnSuccess(o -> log.info("Orden [id={}] → {}", o.getId(), o.getStatus()))
                .doOnError(e -> log.error("Error procesando stock orden [id={}]: {}", order.getId(), e.getMessage()))
                .subscribe();
    }
}

