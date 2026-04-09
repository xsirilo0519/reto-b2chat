package com.b2chat.order_manager.reactive.web.order.mapper;

import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.reactive.web.order.dto.OrderItemSummaryDto;
import com.b2chat.order_manager.reactive.web.order.dto.OrderSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserOrdersMapper Tests")
class UserOrdersMapperTest {

    private final UserOrdersMapper mapper = UserOrdersMapper.INSTANCE;

    // ── helpers ──────────────────────────────────────────────────────────────

    private OrderItemEntity buildItem(String name, String desc, int qty,
                                      BigDecimal unitPrice, BigDecimal total) {
        return OrderItemEntity.builder()
                .id(1L).productId(10L)
                .productName(name).productDescription(desc)
                .quantity(qty).unitPrice(unitPrice).total(total)
                .build();
    }

    private OrderEntity buildOrder(Long id, OrderStatus status, BigDecimal amount,
                                   LocalDateTime createdAt, List<OrderItemEntity> items) {
        return OrderEntity.builder()
                .id(id).userId(1L).status(status)
                .totalAmount(amount).createdAt(createdAt)
                .items(items)
                .build();
    }

    // ── toItemSummary ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("toItemSummary - debe mapear todos los campos del item correctamente")
    void toItemSummary_shouldMapAllFields() {
        OrderItemEntity item = buildItem("Laptop", "Gaming laptop", 2,
                new BigDecimal("500.00"), new BigDecimal("1000.00"));

        OrderItemSummaryDto dto = mapper.toItemSummary(item);

        assertThat(dto.getProductName()).isEqualTo("Laptop");
        assertThat(dto.getProductDescription()).isEqualTo("Gaming laptop");
        assertThat(dto.getQuantity()).isEqualTo(2);
        assertThat(dto.getUnitPrice()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("toItemSummary - el campo 'total' de la entidad debe mapearse como 'subtotal'")
    void toItemSummary_totalShouldBecomeSubtotal() {
        OrderItemEntity item = buildItem("Mouse", "Wireless", 3,
                new BigDecimal("20.00"), new BigDecimal("60.00"));

        OrderItemSummaryDto dto = mapper.toItemSummary(item);

        assertThat(dto.getSubtotal()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("toItemSummary - subtotal debe ser distinto al unitPrice cuando qty > 1")
    void toItemSummary_subtotalShouldDifferFromUnitPrice_whenQuantityIsGreaterThanOne() {
        OrderItemEntity item = buildItem("Teclado", "Mecánico", 4,
                new BigDecimal("50.00"), new BigDecimal("200.00"));

        OrderItemSummaryDto dto = mapper.toItemSummary(item);

        assertThat(dto.getUnitPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(dto.getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(dto.getSubtotal()).isNotEqualByComparingTo(dto.getUnitPrice());
    }

    // ── toOrderSummary ────────────────────────────────────────────────────────

    @Test
    @DisplayName("toOrderSummary - el campo 'id' de la entidad debe mapearse como 'orderId'")
    void toOrderSummary_idShouldBecomeOrderId() {
        OrderEntity order = buildOrder(42L, OrderStatus.PENDING,
                new BigDecimal("100.00"), LocalDateTime.now(), List.of());

        OrderSummaryDto dto = mapper.toOrderSummary(order);

        assertThat(dto.getOrderId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("toOrderSummary - debe mapear status, totalAmount y createdAt correctamente")
    void toOrderSummary_shouldMapStatusAmountAndDate() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 8, 10, 0);
        OrderEntity order = buildOrder(1L, OrderStatus.COMPLETED,
                new BigDecimal("350.00"), createdAt, List.of());

        OrderSummaryDto dto = mapper.toOrderSummary(order);

        assertThat(dto.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(dto.getTotalAmount()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("toOrderSummary - debe mapear los items usando toItemSummary (total → subtotal)")
    void toOrderSummary_shouldMapItemsUsingToItemSummary() {
        OrderItemEntity item = buildItem("Monitor", "4K 27\"", 1,
                new BigDecimal("800.00"), new BigDecimal("800.00"));
        OrderEntity order = buildOrder(1L, OrderStatus.COMPLETED,
                new BigDecimal("800.00"), LocalDateTime.now(), List.of(item));

        OrderSummaryDto dto = mapper.toOrderSummary(order);

        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getProductName()).isEqualTo("Monitor");
        assertThat(dto.getItems().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    @DisplayName("toOrderSummary - debe retornar lista vacía de items cuando la orden no tiene items")
    void toOrderSummary_shouldReturnEmptyItemsList_whenOrderHasNoItems() {
        OrderEntity order = buildOrder(1L, OrderStatus.PENDING,
                BigDecimal.ZERO, LocalDateTime.now(), List.of());

        OrderSummaryDto dto = mapper.toOrderSummary(order);

        assertThat(dto.getItems()).isEmpty();
    }

    @Test
    @DisplayName("toOrderSummary - debe mapear múltiples items correctamente")
    void toOrderSummary_shouldMapMultipleItemsCorrectly() {
        List<OrderItemEntity> items = List.of(
                buildItem("Laptop", "16GB RAM", 1, new BigDecimal("1200.00"), new BigDecimal("1200.00")),
                buildItem("Mouse", "Inalámbrico", 2, new BigDecimal("25.00"), new BigDecimal("50.00"))
        );
        OrderEntity order = buildOrder(1L, OrderStatus.COMPLETED,
                new BigDecimal("1250.00"), LocalDateTime.now(), items);

        OrderSummaryDto dto = mapper.toOrderSummary(order);

        assertThat(dto.getItems()).hasSize(2);
        assertThat(dto.getItems().get(0).getProductName()).isEqualTo("Laptop");
        assertThat(dto.getItems().get(1).getProductName()).isEqualTo("Mouse");
        assertThat(dto.getItems().get(1).getSubtotal()).isEqualByComparingTo(new BigDecimal("50.00"));
    }
}

