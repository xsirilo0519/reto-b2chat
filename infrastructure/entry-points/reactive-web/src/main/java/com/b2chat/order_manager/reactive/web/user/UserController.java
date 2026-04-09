package com.b2chat.order_manager.reactive.web.user;

import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.reactive.web.order.dto.UserOrdersResponseDto;
import com.b2chat.order_manager.reactive.web.order.mapper.UserOrdersMapper;
import com.b2chat.order_manager.reactive.web.user.dto.UserDto;
import com.b2chat.order_manager.reactive.web.user.dto.UserResponseDto;
import com.b2chat.order_manager.reactive.web.user.mapper.UserMapper;
import com.b2chat.order_manager.usecase.OrdersUseCase;
import com.b2chat.order_manager.usecase.UsersUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UsersUseCase usersUseCase;
    private final OrdersUseCase ordersUseCase;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserResponseDto>> getUsersById(@PathVariable("id") Long id) {
        return usersUseCase.getUserByIdUseCase(id)
                .map(UserMapper.INSTANCE::toResponse)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<UserResponseDto>> createUser(@Valid @RequestBody UserDto user) {
        UserEntity userEntity = UserMapper.INSTANCE.toEntity(user);
        return usersUseCase.createUserUseCase(userEntity)
                .map(UserMapper.INSTANCE::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{userId}/orders")
    public Mono<ResponseEntity<UserOrdersResponseDto>> getOrdersByUserId(
            @PathVariable("userId") Long userId) {

        return ordersUseCase.getOrdersByUserIdUseCase(userId)
                .map(UserOrdersMapper.INSTANCE::toOrderSummary)
                .collectList()
                .map(orders -> UserOrdersResponseDto.builder()
                        .userId(userId)
                        .totalOrders(orders.size())
                        .orders(orders)
                        .build())
                .map(ResponseEntity::ok);
    }
}