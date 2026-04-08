package com.b2chat.order_manager.repository.order.repository;

import com.b2chat.order_manager.repository.order.data.OrderData;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderDataRepository extends ReactiveCrudRepository<OrderData, Long> {
}

