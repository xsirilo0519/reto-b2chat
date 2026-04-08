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

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrdersUseCase ordersUseCase;

    @PostMapping
    public Mono<ResponseEntity<OrderResponseDto>> createOrder(@Valid @RequestBody OrderDto orderDto) {
        return ordersUseCase.createOrderUseCase(OrderMapper.INSTANCE.toEntity(orderDto))
                .map(OrderMapper.INSTANCE::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
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

