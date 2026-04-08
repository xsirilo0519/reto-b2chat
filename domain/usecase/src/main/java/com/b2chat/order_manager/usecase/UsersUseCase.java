package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.domain.users.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class UsersUseCase {

    private final UserGateway userGateway;

    public Mono<UserEntity> createUserUseCase(UserEntity userEntity) {
        return Mono.just(userEntity.toBuilder()
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .flatMap(userGateway::createUser)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("Error creating user"))));
    }

    public Mono<UserEntity> getUserByIdUseCase(Long userId) {
        return userGateway.getUserById(userId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("User not found with id: " + userId))));
    }
}
