package com.b2chat.order_manager.domain.notification;

import com.b2chat.order_manager.domain.order.OrderEntity;
import reactor.core.publisher.Mono;

public interface OrderNotificationGateway {
    Mono<Void> notifyOrderReceived(OrderEntity order, String userEmail, String userName);
    Mono<Void> notifyOrderCompleted(OrderEntity order, String userEmail, String userName);
    Mono<Void> notifyOrderCancelled(OrderEntity order, String userEmail, String userName, String reason);
}

