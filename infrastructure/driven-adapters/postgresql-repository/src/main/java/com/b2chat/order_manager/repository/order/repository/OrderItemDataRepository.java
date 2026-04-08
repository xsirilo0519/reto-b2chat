package com.b2chat.order_manager.repository.order.repository;

import com.b2chat.order_manager.repository.order.data.OrderItemData;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderItemDataRepository extends ReactiveCrudRepository<OrderItemData, Long> {
    Flux<OrderItemData> findByOrderId(Long orderId);
}

