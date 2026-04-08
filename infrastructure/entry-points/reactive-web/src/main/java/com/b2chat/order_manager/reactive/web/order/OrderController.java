package com.b2chat.order_manager.reactive.web.order;

import com.b2chat.order_manager.reactive.web.order.dto.OrderDto;
import com.b2chat.order_manager.reactive.web.order.dto.OrderResponseDto;
import com.b2chat.order_manager.reactive.web.order.dto.UpdateOrderStatusDto;
import com.b2chat.order_manager.reactive.web.order.mapper.OrderMapper;
import com.b2chat.order_manager.usecase.OrdersUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrdersUseCase ordersUseCase;

    @PostMapping
    public Mono<ResponseEntity<Map<String, String>>> createOrder(@Valid @RequestBody OrderDto orderDto) {
        return ordersUseCase.receiveOrderUseCase(OrderMapper.INSTANCE.toEntity(orderDto))
                .thenReturn(ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .<Map<String, String>>body(Map.of("message", "Pedido recibido. Será procesado en breve.")));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrderResponseDto>> getOrderById(@PathVariable("id") Long id) {
        return ordersUseCase.getOrderByIdUseCase(id)
                .map(OrderMapper.INSTANCE::toResponse)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<OrderResponseDto>> updateOrderStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateOrderStatusDto dto) {
        return ordersUseCase.updateOrderStatusUseCase(id, dto.getStatus())
                .map(OrderMapper.INSTANCE::toResponse)
                .map(ResponseEntity::ok);
    }
}
