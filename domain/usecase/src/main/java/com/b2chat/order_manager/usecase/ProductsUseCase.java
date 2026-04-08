package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ProductsUseCase {

    private final ProductGateway productGateway;

    public Flux<ProductEntity> getAllProductsUseCase(int page) {
        return productGateway.findAllProducts(page);
    }

    public Mono<ProductEntity> createProductUseCase(ProductEntity productEntity) {
        return Mono.just(productEntity.toBuilder()
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .flatMap(productGateway::createProduct)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("Error creating product"))));
    }

    public Mono<ProductEntity> updateProductUseCase(Long id, ProductEntity productEntity) {
        return productGateway.updateProduct(id, productEntity.toBuilder()
                        .updatedAt(LocalDateTime.now())
                        .build())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("Product not found with id: " + id))));
    }

    public Mono<String> deleteProductUseCase(Long id) {
        return productGateway.deleteProductById(id)
                .onErrorResume(e -> Mono.error(new RuntimeException("Error deleting product with id: " + id)));
    }
}

