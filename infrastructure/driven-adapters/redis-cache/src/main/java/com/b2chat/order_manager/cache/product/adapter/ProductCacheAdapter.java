package com.b2chat.order_manager.cache.product.adapter;

import com.b2chat.order_manager.domain.products.entity.ProductEntity;
import com.b2chat.order_manager.domain.products.gateway.ProductCacheGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheAdapter implements ProductCacheGateway {

    private final ReactiveRedisTemplate<String, ProductEntity> redisTemplate;

    private static final String CACHE_PREFIX = "product:";
    private static final Duration TTL = Duration.ofMinutes(10);

    @Override
    public Mono<ProductEntity> getProduct(Long id) {
        String key = CACHE_PREFIX + id;
        return redisTemplate.opsForValue().get(key)
                .doOnNext(p -> log.info("Cache HIT para producto id={}", id))
                .doOnSuccess(p -> { if (p == null) log.info("Cache MISS para producto id={}", id); });
    }

    @Override
    public Mono<ProductEntity> saveProduct(Long id, ProductEntity product) {
        String key = CACHE_PREFIX + id;
        return redisTemplate.opsForValue().set(key, product, TTL)
                .doOnSuccess(ok -> log.info("Producto id={} guardado en caché (TTL={}min)", id, TTL.toMinutes()))
                .thenReturn(product);
    }

    @Override
    public Mono<Void> evictProduct(Long id) {
        String key = CACHE_PREFIX + id;
        return redisTemplate.opsForValue().delete(key)
                .doOnSuccess(ok -> log.info("Caché eviccionada para producto id={}", id))
                .then();
    }
}

