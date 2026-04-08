package com.b2chat.order_manager.domain.products.gateway;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductGateway {
    Flux<ProductEntity> findAllProducts(int page);
    Mono<ProductEntity> createProduct(ProductEntity productEntity);
    Mono<ProductEntity> updateProduct(Long id, ProductEntity productEntity);
    Mono<String> deleteProductById(Long id);
}
