package com.b2chat.order_manager.repository.order.adapter;

import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderGateway;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.repository.order.data.OrderItemData;
import com.b2chat.order_manager.repository.order.mapper.OrderDataMapper;
import com.b2chat.order_manager.repository.order.mapper.OrderItemDataMapper;
import com.b2chat.order_manager.repository.order.repository.OrderDataRepository;
import com.b2chat.order_manager.repository.order.repository.OrderItemDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OrderAdapter implements OrderGateway {

    private final OrderDataRepository orderDataRepository;
    private final OrderItemDataRepository orderItemDataRepository;

    @Override
    public Mono<OrderEntity> createOrder(OrderEntity orderEntity) {
        return orderDataRepository.save(OrderDataMapper.INSTANCE.toData(orderEntity))
                .flatMap(savedOrder ->
                        Flux.fromIterable(orderEntity.getItems())
                                .flatMap(item -> {
                                    OrderItemData itemData = OrderItemDataMapper.INSTANCE.toData(item);
                                    itemData.setOrderId(savedOrder.getId());
                                    return orderItemDataRepository.save(itemData);
                                })
                                .map(OrderItemDataMapper.INSTANCE::toDomain)
                                .collectList()
                                .map(savedItems -> {
                                    OrderEntity result = OrderDataMapper.INSTANCE.toDomain(savedOrder);
                                    result.setItems(savedItems);
                                    return result;
                                })
                );
    }

    @Override
    public Mono<OrderEntity> getOrderById(Long id) {
        return orderDataRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Orden", id)))
                .flatMap(orderData ->
                        orderItemDataRepository.findByOrderId(id)
                                .map(OrderItemDataMapper.INSTANCE::toDomain)
                                .collectList()
                                .map(items -> {
                                    OrderEntity order = OrderDataMapper.INSTANCE.toDomain(orderData);
                                    order.setItems(items);
                                    return order;
                                })
                );
    }

    @Override
    public Mono<OrderEntity> updateOrderStatus(Long id, OrderStatus status) {
        return orderDataRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Orden", id)))
                .flatMap(existing -> {
                    existing.setStatus(status.name());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return orderDataRepository.save(existing);
                })
                .flatMap(savedOrder ->
                        orderItemDataRepository.findByOrderId(id)
                                .map(OrderItemDataMapper.INSTANCE::toDomain)
                                .collectList()
                                .map(items -> {
                                    OrderEntity order = OrderDataMapper.INSTANCE.toDomain(savedOrder);
                                    order.setItems(items);
                                    return order;
                                })
                );
    }
}

