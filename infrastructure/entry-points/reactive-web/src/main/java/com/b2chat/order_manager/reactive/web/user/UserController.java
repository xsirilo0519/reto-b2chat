package com.b2chat.order_manager.reactive.web.user;

import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.reactive.web.user.dto.UserDto;
import com.b2chat.order_manager.reactive.web.user.dto.UserResponseDto;
import com.b2chat.order_manager.reactive.web.user.mapper.UserMapper;
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

}