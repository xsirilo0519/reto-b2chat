package com.b2chat.order_manager.repository.user.adapter;

import com.b2chat.order_manager.domain.exception.DuplicateEmailException;
import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.domain.users.gateway.UserGateway;
import com.b2chat.order_manager.repository.user.mapper.UserDataMapper;
import com.b2chat.order_manager.repository.user.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserAdapter implements UserGateway {

    private final UserDataRepository userDataRepository;

    @Override
    public Mono<UserEntity> createUser(UserEntity userEntity) {
        return userDataRepository.save(UserDataMapper.INSTANCE.toData(userEntity))
                .map(UserDataMapper.INSTANCE::toDomain)
                .onErrorResume(DataIntegrityViolationException.class,
                        e -> Mono.error(new DuplicateEmailException(userEntity.getEmail())));
    }

    @Override
    public Mono<UserEntity> getUserById(Long userId) {
        return userDataRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Usuario", userId)))
                .map(UserDataMapper.INSTANCE::toDomain);
    }
}
