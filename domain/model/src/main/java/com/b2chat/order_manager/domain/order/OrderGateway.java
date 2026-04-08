package com.b2chat.order_manager.domain.order;

import reactor.core.publisher.Mono;

public interface OrderGateway {
    Mono<OrderEntity> createOrder(OrderEntity orderEntity);
    Mono<OrderEntity> getOrderById(Long id);
    Mono<OrderEntity> updateOrderStatus(Long id, OrderStatus status);
}

