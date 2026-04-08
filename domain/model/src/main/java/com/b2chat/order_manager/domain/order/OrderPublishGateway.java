package com.b2chat.order_manager.domain.order;

import reactor.core.publisher.Mono;

public interface OrderPublishGateway {
    Mono<Void> publishOrderForProcessing(OrderEntity order);
    Mono<Void> publishOrderForStockProcessing(OrderEntity order);
}

