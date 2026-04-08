package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.exception.InsufficientStockException;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderGateway;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import com.b2chat.order_manager.domain.users.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class OrdersUseCase {

    private final OrderGateway orderGateway;
    private final ProductGateway productGateway;
    private final UserGateway userGateway;

    @Transactional
    public Mono<OrderEntity> createOrderUseCase(OrderEntity orderRequest) {
        AtomicReference<BigDecimal> total = new AtomicReference<>(BigDecimal.ZERO);

        return userGateway.getUserById(orderRequest.getUserId())
                .flatMap(user -> processItems(orderRequest, total))
                .flatMap(enrichedItems ->
                        orderGateway.createOrder(orderRequest.toBuilder()
                                .status(OrderStatus.PENDING)
                                .totalAmount(total.get())
                                .items(enrichedItems)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build())
                )
                .flatMap(savedOrder ->
                        Flux.fromIterable(savedOrder.getItems())
                                .flatMap(item -> productGateway.decrementStock(item.getProductId(), item.getQuantity()))
                                .then(Mono.just(savedOrder))
                );
    }

    private Mono<List<OrderItemEntity>> processItems(OrderEntity orderRequest, AtomicReference<BigDecimal> total) {
        return Flux.fromIterable(orderRequest.getItems())
                .flatMapSequential(item ->
                        productGateway.findProductById(item.getProductId())
                                .flatMap(product -> {
                                    if (product.getStockQuantity() < item.getQuantity()) {
                                        return Mono.error(new InsufficientStockException(
                                                product.getName(),
                                                item.getQuantity(),
                                                product.getStockQuantity()));
                                    }
                                    BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                                    total.accumulateAndGet(itemTotal, BigDecimal::add);
                                    return Mono.just(item.toBuilder()
                                            .unitPrice(product.getPrice())
                                            .total(itemTotal)
                                            .build());
                                })
                )
                .collectList();
    }

    public Mono<OrderEntity> getOrderByIdUseCase(Long id) {
        return orderGateway.getOrderById(id);
    }

    public Mono<OrderEntity> updateOrderStatusUseCase(Long id, OrderStatus status) {
        return orderGateway.updateOrderStatus(id, status);
    }
}
