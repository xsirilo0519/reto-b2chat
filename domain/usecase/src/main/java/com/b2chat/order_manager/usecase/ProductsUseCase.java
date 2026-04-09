package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.domain.products.gateway.ProductCacheGateway;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ProductsUseCase {

    private final ProductGateway productGateway;
    private final ProductCacheGateway productCacheGateway;

    public Flux<ProductEntity> getAllProductsUseCase(int page) {
        return productGateway.findAllProducts(page);
    }

    public Mono<ProductEntity> getProductByIdUseCase(Long id) {
        return productCacheGateway.getProduct(id)
                .switchIfEmpty(Mono.defer(() ->productGateway.findProductById(id)
                        .flatMap(product -> productCacheGateway.saveProduct(id, product)))

                );
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
                .flatMap(updated -> productCacheGateway.evictProduct(id).thenReturn(updated))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("Product not found with id: " + id))));
    }

    public Mono<String> deleteProductUseCase(Long id) {
        return productGateway.deleteProductById(id)
                .flatMap(result -> productCacheGateway.evictProduct(id).thenReturn(result))
                .onErrorResume(e -> Mono.error(new RuntimeException("Error deleting product with id: " + id)));
    }
}
