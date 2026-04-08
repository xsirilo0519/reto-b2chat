package com.b2chat.order_manager.repository.product.repository;

import com.b2chat.order_manager.repository.product.data.ProductData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductDataRepository extends ReactiveCrudRepository<ProductData, Long> {
    Flux<ProductData> findAllBy(Pageable pageable);

    @Modifying
    @Query("UPDATE products SET stock_quantity = stock_quantity - :quantity, updated_at = NOW() WHERE id = :id")
    Mono<Integer> decrementStockQuery(@Param("id") Long id, @Param("quantity") Integer quantity);
}
