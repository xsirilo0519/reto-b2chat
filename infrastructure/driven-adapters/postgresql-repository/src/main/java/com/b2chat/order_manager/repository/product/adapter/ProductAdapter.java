package com.b2chat.order_manager.repository.product.adapter;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import com.b2chat.order_manager.repository.product.mapper.ProductDataMapper;
import com.b2chat.order_manager.repository.product.repository.ProductDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Sort;
@Component
@RequiredArgsConstructor
public class ProductAdapter implements ProductGateway {

    private final ProductDataRepository productDataRepository;

    private static final int BATCH_SIZE = 100;

    @Override
    public Flux<ProductEntity> findAllProducts(int page) {
        return productDataRepository.findAllBy(PageRequest.of(page, BATCH_SIZE, Sort.by("id").ascending()))
                .map(ProductDataMapper.INSTANCE::toDomain)
                .onErrorResume(e -> Flux.error(new RuntimeException("Error fetching products: " + e.getMessage())));
    }

    @Override
    public Mono<ProductEntity> createProduct(ProductEntity productEntity) {
        return productDataRepository.save(ProductDataMapper.INSTANCE.toData(productEntity))
                .map(ProductDataMapper.INSTANCE::toDomain)
                .onErrorResume(e -> Mono.error(new RuntimeException("Error creating product: " + e.getMessage())));
    }

    @Override
    public Mono<ProductEntity> updateProduct(Long id, ProductEntity productEntity) {
        return productDataRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Product not found with id: " + id)))
                .flatMap(existing -> {
                    existing.setName(productEntity.getName());
                    existing.setDescription(productEntity.getDescription());
                    existing.setPrice(productEntity.getPrice());
                    existing.setStockQuantity(productEntity.getStockQuantity());
                    existing.setUpdatedAt(productEntity.getUpdatedAt());
                    return productDataRepository.save(existing);
                })
                .map(ProductDataMapper.INSTANCE::toDomain)
                .onErrorResume(e -> Mono.error(new RuntimeException(e.getMessage())));
    }

    @Override
    public Mono<String> deleteProductById(Long id) {
        return productDataRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Product not found with id: " + id)))
                .flatMap(existing -> productDataRepository.deleteById(id)
                        .then(Mono.just("Product deleted successfully")))
                .onErrorResume(e -> Mono.error(new RuntimeException(e.getMessage())));
    }
}

