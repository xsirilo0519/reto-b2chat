package com.b2chat.order_manager.domain.products.gateway;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import reactor.core.publisher.Mono;

public interface ProductCacheGateway {
    Mono<ProductEntity> getProduct(Long id);
    Mono<ProductEntity> saveProduct(Long id, ProductEntity product);
    Mono<Void> evictProduct(Long id);
}

