package com.b2chat.order_manager.repository.order.adapter;

import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderGateway;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.repository.order.data.OrderItemData;
import com.b2chat.order_manager.repository.order.mapper.OrderDataMapper;
import com.b2chat.order_manager.repository.order.mapper.OrderItemDataMapper;
import com.b2chat.order_manager.repository.order.repository.OrderDataRepository;
import com.b2chat.order_manager.repository.order.repository.OrderItemDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderAdapter implements OrderGateway {

    private static final String ENRICHED_ORDERS_BY_USER_SQL = """
            SELECT o.id           AS order_id,
                   o.user_id,
                   o.total_amount,
                   o.status,
                   o.created_at,
                   oi.id         AS item_id,
                   oi.product_id,
                   oi.quantity,
                   oi.unit_price,
                   oi.total      AS item_total,
                   p.name        AS product_name,
                   p.description AS product_description
            FROM orders o
                     INNER JOIN order_items oi ON o.id = oi.order_id
                     INNER JOIN products p ON oi.product_id = p.id
            WHERE o.user_id = :userId
            ORDER BY o.created_at DESC, o.id, oi.id
            """;

    private final OrderDataRepository orderDataRepository;
    private final OrderItemDataRepository orderItemDataRepository;
    private final DatabaseClient databaseClient;

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
                .flatMap(orderData -> enrichWithItems(orderData.getId(),
                        OrderDataMapper.INSTANCE.toDomain(orderData)));
    }

    @Override
    public Mono<OrderEntity> updateOrderStatus(Long id, OrderStatus status) {
        return orderDataRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Orden", id)))
                .flatMap(existing -> {
                    existing.setStatus(status.name());
                    existing.setUpdatedAt(LocalDateTime.now());
                    if (status == OrderStatus.COMPLETED) {
                        existing.setCompleted(true);
                    }
                    return orderDataRepository.save(existing);
                })
                .flatMap(savedOrder -> enrichWithItems(savedOrder.getId(),
                        OrderDataMapper.INSTANCE.toDomain(savedOrder)));
    }

    @Override
    public Flux<OrderEntity> getOrdersByUserId(Long userId) {
        return databaseClient.sql(ENRICHED_ORDERS_BY_USER_SQL)
                .bind("userId", userId)
                .map((row, meta) -> new OrderFlatRow(
                        row.get("order_id", Long.class),
                        row.get("user_id", Long.class),
                        row.get("total_amount", BigDecimal.class),
                        row.get("status", String.class),
                        row.get("created_at", LocalDateTime.class),
                        row.get("item_id", Long.class),
                        row.get("product_id", Long.class),
                        row.get("quantity", Integer.class),
                        row.get("unit_price", BigDecimal.class),
                        row.get("item_total", BigDecimal.class),
                        row.get("product_name", String.class),
                        row.get("product_description", String.class)
                ))
                .all()
                .collectList()
                .flatMapMany(rows -> {
                    Map<Long, List<OrderFlatRow>> grouped = rows.stream()
                            .collect(Collectors.groupingBy(
                                    OrderFlatRow::orderId,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));
                    return Flux.fromIterable(grouped.values())
                            .map(this::flatRowsToOrderEntity);
                });
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Mono<OrderEntity> enrichWithItems(Long orderId, OrderEntity order) {
        return orderItemDataRepository.findByOrderId(orderId)
                .map(OrderItemDataMapper.INSTANCE::toDomain)
                .collectList()
                .map(items -> {
                    order.setItems(items);
                    return order;
                });
    }

    private OrderEntity flatRowsToOrderEntity(List<OrderFlatRow> rows) {
        OrderFlatRow first = rows.get(0);

        List<OrderItemEntity> items = rows.stream()
                .map(r -> OrderItemEntity.builder()
                        .id(r.itemId())
                        .orderId(first.orderId())
                        .productId(r.productId())
                        .quantity(r.quantity())
                        .unitPrice(r.unitPrice())
                        .total(r.itemTotal())
                        .productName(r.productName())
                        .productDescription(r.productDescription())
                        .build())
                .collect(Collectors.toList());

        return OrderEntity.builder()
                .id(first.orderId())
                .userId(first.userId())
                .status(OrderStatus.valueOf(first.status()))
                .totalAmount(first.totalAmount())
                .createdAt(first.createdAt())
                .items(items)
                .build();
    }

    // ── flat row record (one row per item from the JOIN) ─────────────────────
    private record OrderFlatRow(
            Long orderId,
            Long userId,
            BigDecimal totalAmount,
            String status,
            LocalDateTime createdAt,
            Long itemId,
            Long productId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal itemTotal,
            String productName,
            String productDescription
    ) {}
}
