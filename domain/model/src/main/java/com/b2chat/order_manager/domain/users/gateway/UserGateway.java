package com.b2chat.order_manager.domain.users.gateway;

import com.b2chat.order_manager.domain.users.entity.UserEntity;
import reactor.core.publisher.Mono;

public interface UserGateway {
     Mono<UserEntity> createUser(UserEntity userEntity);
     Mono<UserEntity> getUserById(String userId);
}
