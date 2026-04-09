package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.exception.InsufficientStockException;
import com.b2chat.order_manager.domain.notification.OrderNotificationGateway;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderGateway;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderPublishGateway;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.domain.products.gateway.ProductGateway;
import com.b2chat.order_manager.domain.users.entity.UserEntity;
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
    private final OrderNotificationGateway notificationGateway;
    private final OrderPublishGateway publishGateway;

    public Mono<Void> receiveOrderUseCase(OrderEntity orderRequest) {
        return validateOrderRequest(orderRequest)
                .then(Mono.defer(() -> publishGateway.publishOrderForProcessing(orderRequest)));
    }

    public Mono<OrderEntity> createOrderUseCase(OrderEntity orderRequest) {
        AtomicReference<BigDecimal> total = new AtomicReference<>(BigDecimal.ZERO);

        return userGateway.getUserById(orderRequest.getUserId())
                .flatMap(user -> enrichItemsWithPrices(orderRequest, total)
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
                                notificationGateway.notifyOrderReceived(savedOrder, user.getEmail(), user.getName())
                                        .thenReturn(savedOrder)
                        )
                );
    }


    public Mono<OrderEntity> updateOrderStatusUseCase(Long id, OrderStatus status) {
        if (status == OrderStatus.PROCESSING) {
            return orderGateway.updateOrderStatus(id, OrderStatus.PROCESSING)
                    .flatMap(order ->
                            publishGateway.publishOrderForStockProcessing(order).thenReturn(order)
                    );
        }
        return orderGateway.updateOrderStatus(id, status);
    }

    @Transactional
    public Mono<OrderEntity> processOrderStock(OrderEntity order) {
        if (order.isCompleted()) {
            return orderGateway.updateOrderStatus(order.getId(), OrderStatus.COMPLETED)
                    .flatMap(completed ->
                            userGateway.getUserById(completed.getUserId())
                                    .flatMap(user -> notificationGateway
                                            .notifyOrderCompleted(completed, user.getEmail(), user.getName())
                                            .thenReturn(completed))
                    );
        }
        return userGateway.getUserById(order.getUserId())
                .flatMap(user -> validateStockAndProcess(order, user));
    }

    public Mono<OrderEntity> getOrderByIdUseCase(Long id) {
        return orderGateway.getOrderById(id);
    }

    public Flux<OrderEntity> getOrdersByUserIdUseCase(Long userId) {
        return orderGateway.getOrdersByUserId(userId);
    }

    public Mono<Void> validateOrderRequest(OrderEntity orderRequest) {
        return userGateway.getUserById(orderRequest.getUserId())
                .flatMap(user -> Flux.fromIterable(orderRequest.getItems())
                        .flatMapSequential(item -> productGateway.findProductById(item.getProductId()))
                        .then()
                );
    }

    private Mono<List<OrderItemEntity>> enrichItemsWithPrices(OrderEntity orderRequest,
                                                               AtomicReference<BigDecimal> total) {
        return Flux.fromIterable(orderRequest.getItems())
                .flatMapSequential(item ->
                        productGateway.findProductById(item.getProductId())
                                .map(product -> {
                                    BigDecimal itemTotal = product.getPrice()
                                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                                    total.accumulateAndGet(itemTotal, BigDecimal::add);
                                    return item.toBuilder()
                                            .unitPrice(product.getPrice())
                                            .total(itemTotal)
                                            .build();
                                })
                )
                .collectList();
    }

    private Mono<OrderEntity> validateStockAndProcess(OrderEntity order, UserEntity user) {
        return Flux.fromIterable(order.getItems())
                .flatMapSequential(item ->
                        productGateway.findProductById(item.getProductId())
                                .flatMap(product -> {
                                    if (product.getStockQuantity() < item.getQuantity()) {
                                        return Mono.error(new InsufficientStockException(
                                                product.getName(),
                                                item.getQuantity(),
                                                product.getStockQuantity()));
                                    }
                                    return Mono.just(item);
                                })
                )
                .collectList()
                .flatMap(validItems ->
                        Flux.fromIterable(order.getItems())
                                .flatMap(item -> productGateway.decrementStock(item.getProductId(), item.getQuantity()))
                                .then(orderGateway.updateOrderStatus(order.getId(), OrderStatus.COMPLETED))
                                .flatMap(completed ->
                                        notificationGateway.notifyOrderCompleted(completed, user.getEmail(), user.getName())
                                                .thenReturn(completed)
                                )
                )
                .onErrorResume(InsufficientStockException.class, ex ->
                        orderGateway.updateOrderStatus(order.getId(), OrderStatus.CANCELLED)
                                .flatMap(cancelled ->
                                        notificationGateway.notifyOrderCancelled(
                                                cancelled, user.getEmail(), user.getName(), ex.getMessage())
                                                .thenReturn(cancelled)
                                )
                );
    }
}
