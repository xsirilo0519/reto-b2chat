package com.b2chat.order_manager.repository.order.adapter;

import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.order.OrderEntity;
import com.b2chat.order_manager.domain.order.OrderItemEntity;
import com.b2chat.order_manager.domain.order.OrderStatus;
import com.b2chat.order_manager.repository.order.data.OrderData;
import com.b2chat.order_manager.repository.order.data.OrderItemData;
import com.b2chat.order_manager.repository.order.repository.OrderDataRepository;
import com.b2chat.order_manager.repository.order.repository.OrderItemDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderAdapter Tests")
class OrderAdapterTest {

    @Mock private OrderDataRepository orderDataRepository;
    @Mock private OrderItemDataRepository orderItemDataRepository;

    private OrderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrderAdapter(orderDataRepository, orderItemDataRepository);
    }


    private OrderEntity buildOrderEntity() {
        return OrderEntity.builder()
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(150.0))
                .items(List.of(
                        OrderItemEntity.builder()
                                .productId(10L)
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(50.0))
                                .total(BigDecimal.valueOf(100.0))
                                .build(),
                        OrderItemEntity.builder()
                                .productId(11L)
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(50.0))
                                .total(BigDecimal.valueOf(50.0))
                                .build()
                ))
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }

    private OrderData buildOrderData(Long id) {
        OrderData data = new OrderData();
        data.setId(id);
        data.setUserId(1L);
        data.setStatus("PENDING");
        data.setTotalAmount(BigDecimal.valueOf(150.0));
        data.setCompleted(false);
        data.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        data.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        return data;
    }

    private OrderItemData buildItemData(Long id, Long orderId) {
        OrderItemData data = new OrderItemData();
        data.setId(id);
        data.setOrderId(orderId);
        data.setProductId(10L);
        data.setQuantity(2);
        data.setUnitPrice(BigDecimal.valueOf(50.0));
        data.setTotal(BigDecimal.valueOf(100.0));
        return data;
    }


    @Test
    @DisplayName("createOrder - debe guardar la orden y sus items, y retornar la entidad completa")
    void createOrder_shouldSaveOrderAndItems_andReturnOrderEntity() {
        OrderEntity orderEntity = buildOrderEntity();
        OrderData savedOrderData = buildOrderData(1L);
        OrderItemData savedItemData = buildItemData(100L, 1L);

        when(orderDataRepository.save(any(OrderData.class)))
                .thenReturn(Mono.just(savedOrderData));
        when(orderItemDataRepository.save(any(OrderItemData.class)))
                .thenReturn(Mono.just(savedItemData));

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

        when(orderDataRepository.save(any(OrderData.class)))
                .thenReturn(Mono.just(savedOrderData));
        when(orderItemDataRepository.save(itemCaptor.capture()))
                .thenReturn(Mono.just(buildItemData(1L, 42L)));

        StepVerifier.create(adapter.createOrder(orderEntity))
                .assertNext(result -> assertThat(result.getId()).isEqualTo(42L))
                .verifyComplete();

        itemCaptor.getAllValues().forEach(item ->
                assertThat(item.getOrderId()).isEqualTo(42L)
        );
    }


    @Test
    @DisplayName("getOrderById - debe retornar la orden con sus items cuando existe")
    void getOrderById_shouldReturnOrderWithItems_whenFound() {
        OrderData orderData = buildOrderData(1L);
        OrderItemData itemData = buildItemData(100L, 1L);

        when(orderDataRepository.findById(1L)).thenReturn(Mono.just(orderData));
        when(orderItemDataRepository.findByOrderId(1L)).thenReturn(Flux.just(itemData));

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
                .expectErrorMatches(ex ->
                        ex instanceof ResourceNotFoundException &&
                        ex.getMessage().contains("99"))
                .verify();
    }

    @Test
    @DisplayName("getOrderById - debe retornar la orden con lista vacía si no tiene items")
    void getOrderById_shouldReturnOrderWithEmptyItems_whenNoItems() {
        OrderData orderData = buildOrderData(1L);

        when(orderDataRepository.findById(1L)).thenReturn(Mono.just(orderData));
        when(orderItemDataRepository.findByOrderId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(adapter.getOrderById(1L))
                .assertNext(result -> {
                    assertThat(result.getId()).isEqualTo(1L);
                    assertThat(result.getItems()).isEmpty();
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("updateOrderStatus - debe actualizar el estado y retornar la orden actualizada")
    void updateOrderStatus_shouldUpdateStatusAndReturnOrder() {
        OrderData existing = buildOrderData(1L);
        OrderData updated = buildOrderData(1L);
        updated.setStatus("PROCESSING");

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
        completedData.setStatus("COMPLETED");
        completedData.setCompleted(true);

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
                .expectErrorMatches(ex ->
                        ex instanceof ResourceNotFoundException &&
                        ex.getMessage().contains("99"))
                .verify();
    }
}

