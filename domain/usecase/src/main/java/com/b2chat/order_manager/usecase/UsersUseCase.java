package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.users.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UsersUseCase {
    public Mono<UserEntity> createUser(UserEntity userEntity) {
        return Mono.just(userEntity);
    }

    public Mono<String> getUserById(String userId) {
        return Mono.just("User with id: " + userId);
    }
}
