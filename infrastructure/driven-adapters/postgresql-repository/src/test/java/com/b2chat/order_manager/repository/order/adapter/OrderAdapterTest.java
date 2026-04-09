package com.b2chat.order_manager.repository.order.adapter;

import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.repository.order.data.OrderData;
import com.b2chat.order_manager.repository.order.data.OrderItemData;
import com.b2chat.order_manager.repository.order.repository.OrderDataRepository;
import com.b2chat.order_manager.repository.order.repository.OrderItemDataRepository;
import io.r2dbc.spi.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderAdapter Tests")
class OrderAdapterTest {

    @Mock private OrderDataRepository orderDataRepository;
    @Mock private OrderItemDataRepository orderItemDataRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private DatabaseClient databaseClient;

    private OrderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrderAdapter(orderDataRepository, orderItemDataRepository, databaseClient);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OrderEntity buildOrderEntity() {
        return OrderEntity.builder()
                .userId(1L).status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(150.0))
                .items(List.of(
                        OrderItemEntity.builder().productId(10L).quantity(2)
                                .unitPrice(BigDecimal.valueOf(50.0)).total(BigDecimal.valueOf(100.0)).build(),
                        OrderItemEntity.builder().productId(11L).quantity(1)
                                .unitPrice(BigDecimal.valueOf(50.0)).total(BigDecimal.valueOf(50.0)).build()
                ))
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    private OrderData buildOrderData(Long id) {
        OrderData data = new OrderData();
        data.setId(id); data.setUserId(1L); data.setStatus("PENDING");
        data.setTotalAmount(BigDecimal.valueOf(150.0)); data.setCompleted(false);
        data.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        data.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return data;
    }

    private OrderItemData buildItemData(Long id, Long orderId) {
        OrderItemData data = new OrderItemData();
        data.setId(id); data.setOrderId(orderId); data.setProductId(10L);
        data.setQuantity(2); data.setUnitPrice(BigDecimal.valueOf(50.0));
        data.setTotal(BigDecimal.valueOf(100.0));
        return data;
    }

    // ── createOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrder - debe guardar la orden y sus items, y retornar la entidad completa")
    void createOrder_shouldSaveOrderAndItems_andReturnOrderEntity() {
        OrderEntity orderEntity = buildOrderEntity();
        OrderData savedOrderData = buildOrderData(1L);
        OrderItemData savedItemData = buildItemData(100L, 1L);

        when(orderDataRepository.save(any(OrderData.class))).thenReturn(Mono.just(savedOrderData));
        when(orderItemDataRepository.save(any(OrderItemData.class))).thenReturn(Mono.just(savedItemData));

        StepVerifier.create(adapter.createOrder(orderEntity))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getUserId()).isEqualTo(1L);
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(result.getItems()).hasSize(2);
                })
                .verifyComplete();

        verify(orderDataRepository).save(any(OrderData.class));
    }

    @Test
    @DisplayName("createOrder - debe asignar el orderId correcto a cada item guardado")
    void createOrder_shouldSetOrderIdOnEachItem() {
        OrderEntity orderEntity = buildOrderEntity();
        OrderData savedOrderData = buildOrderData(42L);
        ArgumentCaptor<OrderItemData> itemCaptor = ArgumentCaptor.forClass(OrderItemData.class);

        when(orderDataRepository.save(any(OrderData.class))).thenReturn(Mono.just(savedOrderData));
        when(orderItemDataRepository.save(itemCaptor.capture())).thenReturn(Mono.just(buildItemData(1L, 42L)));

        StepVerifier.create(adapter.createOrder(orderEntity))
                .assertNext(result -> assertThat(result.getId()).isEqualTo(42L))
                .verifyComplete();

        itemCaptor.getAllValues().forEach(item -> assertThat(item.getOrderId()).isEqualTo(42L));
    }

    // ── getOrderById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderById - debe retornar la orden con sus items cuando existe")
    void getOrderById_shouldReturnOrderWithItems_whenFound() {
        when(orderDataRepository.findById(1L)).thenReturn(Mono.just(buildOrderData(1L)));
        when(orderItemDataRepository.findByOrderId(1L)).thenReturn(Flux.just(buildItemData(100L, 1L)));

        StepVerifier.create(adapter.getOrderById(1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(result.getItems()).hasSize(1);
                    assertThat(result.getItems().get(0).getProductId()).isEqualTo(10L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getOrderById - debe lanzar ResourceNotFoundException cuando no existe")
    void getOrderById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(orderDataRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.getOrderById(99L))
                .expectErrorMatches(ex -> ex instanceof ResourceNotFoundException
                        && ex.getMessage().contains("99"))
                .verify();
    }

    @Test
    @DisplayName("getOrderById - debe retornar la orden con lista vacía si no tiene items")
    void getOrderById_shouldReturnOrderWithEmptyItems_whenNoItems() {
        when(orderDataRepository.findById(1L)).thenReturn(Mono.just(buildOrderData(1L)));
        when(orderItemDataRepository.findByOrderId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(adapter.getOrderById(1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getItems()).isEmpty();
                })
                .verifyComplete();
    }

    // ── updateOrderStatus ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOrderStatus - debe actualizar el estado y retornar la orden actualizada")
    void updateOrderStatus_shouldUpdateStatusAndReturnOrder() {
        OrderData existing = buildOrderData(1L);
        OrderData updated  = buildOrderData(1L); updated.setStatus("PROCESSING");

        when(orderDataRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(orderDataRepository.save(any(OrderData.class))).thenReturn(Mono.just(updated));
        when(orderItemDataRepository.findByOrderId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(adapter.updateOrderStatus(1L, OrderStatus.PROCESSING))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateOrderStatus - debe marcar completed=true cuando el estado es COMPLETED")
    void updateOrderStatus_shouldSetCompletedFlag_whenStatusIsCompleted() {
        OrderData existing = buildOrderData(1L);
        ArgumentCaptor<OrderData> captor = ArgumentCaptor.forClass(OrderData.class);
        OrderData completedData = buildOrderData(1L);
        completedData.setStatus("COMPLETED"); completedData.setCompleted(true);

        when(orderDataRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(orderDataRepository.save(captor.capture())).thenReturn(Mono.just(completedData));
        when(orderItemDataRepository.findByOrderId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(adapter.updateOrderStatus(1L, OrderStatus.COMPLETED))
                .assertNext(result -> {
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(result.isCompleted()).isTrue();
                })
                .verifyComplete();

        assertThat(captor.getValue().isCompleted()).isTrue();
    }

    @Test
    @DisplayName("updateOrderStatus - debe lanzar ResourceNotFoundException cuando la orden no existe")
    void updateOrderStatus_shouldThrowResourceNotFoundException_whenNotFound() {
        when(orderDataRepository.findById(eq(99L))).thenReturn(Mono.empty());

        StepVerifier.create(adapter.updateOrderStatus(99L, OrderStatus.CANCELLED))
                .expectErrorMatches(ex -> ex instanceof ResourceNotFoundException
                        && ex.getMessage().contains("99"))
                .verify();
    }


    @Test
    @DisplayName("getOrdersByUserId - debe retornar Flux vacío cuando el usuario no tiene pedidos (JOIN sin filas)")
    void getOrdersByUserId_shouldReturnEmptyFlux_whenNoOrdersExist() {
        when(databaseClient.sql(anyString())
                .bind(anyString(), any())
                .map(any(BiFunction.class))
                .all())
                .thenReturn(Flux.empty());

        StepVerifier.create(adapter.getOrdersByUserId(99L))
                .verifyComplete();
    }

    @Test
    @DisplayName("getOrdersByUserId - debe llamar al SQL con el userId correcto")
    void getOrdersByUserId_shouldBindUserIdInQuery() {
        when(databaseClient.sql(anyString())
                .bind(eq("userId"), eq(1L))
                .map(any(BiFunction.class))
                .all())
                .thenReturn(Flux.empty());

        StepVerifier.create(adapter.getOrdersByUserId(1L))
                .verifyComplete();

        verify(databaseClient, atLeastOnce()).sql(anyString());
    }

    // ── getOrdersByUserId (row-mapping paths) ──────────────────────────────────

    /**
     * Ejecuta el BiFunction real que pasa el adapter a DatabaseClient.map(),
     * usando mocks de Row, para que JaCoCo cubra la construcción de OrderFlatRow
     * y el método privado flatRowsToOrderEntity.
     */
    @Test
    @DisplayName("getOrdersByUserId - debe mapear filas y agruparlas en una OrderEntity con múltiples items")
    @SuppressWarnings("unchecked")
    void getOrdersByUserId_shouldMapRowsIntoOrderEntity_withMultipleItems() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        Row row1 = buildMockRow(1L, 1L, new BigDecimal("150.00"), "PENDING", createdAt,
                10L, 100L, 2, new BigDecimal("50.00"), new BigDecimal("100.00"),
                "Laptop", "Gaming Laptop");
        Row row2 = buildMockRow(1L, 1L, new BigDecimal("150.00"), "PENDING", createdAt,
                11L, 101L, 1, new BigDecimal("50.00"), new BigDecimal("50.00"),
                "Mouse", "Wireless Mouse");

        List<Object> flatRows = new ArrayList<>();
        RowsFetchSpec<Object> fetchSpec = mock(RowsFetchSpec.class);
        when(fetchSpec.all()).thenAnswer(inv -> Flux.fromIterable(flatRows));

        when(databaseClient.sql(anyString())
                .bind(anyString(), any())
                .map(any(BiFunction.class)))
                .thenAnswer(inv -> {
                    BiFunction<Row, Object, Object> fn = inv.getArgument(0);
                    flatRows.add(fn.apply(row1, null));
                    flatRows.add(fn.apply(row2, null));
                    return fetchSpec;
                });

        StepVerifier.create(adapter.getOrdersByUserId(1L))
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(1L);
                    assertThat(order.getUserId()).isEqualTo(1L);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(order.getTotalAmount()).isEqualByComparingTo("150.00");
                    assertThat(order.getCreatedAt()).isEqualTo(createdAt);
                    assertThat(order.getItems()).hasSize(2);
                    assertThat(order.getItems().get(0).getProductName()).isEqualTo("Laptop");
                    assertThat(order.getItems().get(0).getTotal()).isEqualByComparingTo("100.00");
                    assertThat(order.getItems().get(1).getProductName()).isEqualTo("Mouse");
                    assertThat(order.getItems().get(1).getProductDescription()).isEqualTo("Wireless Mouse");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getOrdersByUserId - debe retornar múltiples órdenes agrupadas por orderId")
    @SuppressWarnings("unchecked")
    void getOrdersByUserId_shouldReturnMultipleOrders_whenMultipleOrdersExist() {
        LocalDateTime createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        LocalDateTime createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        Row row1 = buildMockRow(1L, 1L, new BigDecimal("100.00"), "COMPLETED", createdAt1,
                10L, 100L, 1, new BigDecimal("100.00"), new BigDecimal("100.00"),
                "Laptop", "Gaming Laptop");
        Row row2 = buildMockRow(2L, 1L, new BigDecimal("50.00"), "PENDING", createdAt2,
                11L, 101L, 1, new BigDecimal("50.00"), new BigDecimal("50.00"),
                "Mouse", "Wireless Mouse");

        List<Object> flatRows = new ArrayList<>();
        RowsFetchSpec<Object> fetchSpec = mock(RowsFetchSpec.class);
        when(fetchSpec.all()).thenAnswer(inv -> Flux.fromIterable(flatRows));

        when(databaseClient.sql(anyString())
                .bind(anyString(), any())
                .map(any(BiFunction.class)))
                .thenAnswer(inv -> {
                    BiFunction<Row, Object, Object> fn = inv.getArgument(0);
                    flatRows.add(fn.apply(row1, null));
                    flatRows.add(fn.apply(row2, null));
                    return fetchSpec;
                });

        StepVerifier.create(adapter.getOrdersByUserId(1L))
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(1L);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getItems()).hasSize(1);
                    assertThat(order.getItems().get(0).getProductId()).isEqualTo(100L);
                })
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(2L);
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                    assertThat(order.getItems()).hasSize(1);
                    assertThat(order.getItems().get(0).getProductId()).isEqualTo(101L);
                })
                .verifyComplete();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Construye un mock de {@link Row} que devuelve los valores indicados para cada columna. */
    private Row buildMockRow(Long orderId, Long userId, BigDecimal totalAmount,
                             String status, LocalDateTime createdAt,
                             Long itemId, Long productId, Integer quantity,
                             BigDecimal unitPrice, BigDecimal itemTotal,
                             String productName, String productDescription) {
        Row row = mock(Row.class);
        when(row.get("order_id",           Long.class)).thenReturn(orderId);
        when(row.get("user_id",            Long.class)).thenReturn(userId);
        when(row.get("total_amount",       BigDecimal.class)).thenReturn(totalAmount);
        when(row.get("status",             String.class)).thenReturn(status);
        when(row.get("created_at",         LocalDateTime.class)).thenReturn(createdAt);
        when(row.get("item_id",            Long.class)).thenReturn(itemId);
        when(row.get("product_id",         Long.class)).thenReturn(productId);
        when(row.get("quantity",           Integer.class)).thenReturn(quantity);
        when(row.get("unit_price",         BigDecimal.class)).thenReturn(unitPrice);
        when(row.get("item_total",         BigDecimal.class)).thenReturn(itemTotal);
        when(row.get("product_name",       String.class)).thenReturn(productName);
        when(row.get("product_description",String.class)).thenReturn(productDescription);
        return row;
    }
}
