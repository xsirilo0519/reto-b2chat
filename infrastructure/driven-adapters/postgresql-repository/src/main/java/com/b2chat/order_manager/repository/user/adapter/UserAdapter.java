package com.b2chat.order_manager.repository.user.adapter;

import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.domain.users.gateway.UserGateway;
import com.b2chat.order_manager.repository.user.mapper.UserDataMapper;
import com.b2chat.order_manager.repository.user.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class UserAdapter implements UserGateway {

    private final UserDataRepository userDataRepository;

    @Override
    public Mono<UserEntity> createUser(UserEntity userEntity) {
        return userDataRepository.save(UserDataMapper.INSTANCE.toData(userEntity))
                .map(UserDataMapper.INSTANCE::toDomain)
                .onErrorResume(e -> Mono.error(new RuntimeException("Error in db:" + e.getMessage())));
    }

    @Override
    public Mono<UserEntity> getUserById(String userId) {
        return userDataRepository.findById(userId)
                .map(UserDataMapper.INSTANCE::toDomain)
                .onErrorResume(e -> Mono.error(new RuntimeException("Error in db:" + e.getMessage())));
    }
}
