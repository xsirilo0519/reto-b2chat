package com.b2chat.order_manager.repository.product.repository;

import com.b2chat.order_manager.repository.product.data.ProductData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProductDataRepository extends ReactiveCrudRepository<ProductData, Long> {
    Flux<ProductData> findAllBy(Pageable pageable);
}

