package com.b2chat.order_manager.reactive.web.user;

import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.reactive.web.user.dto.UserDto;
import com.b2chat.order_manager.reactive.web.user.mapper.UserMapper;
import com.b2chat.order_manager.usecase.UsersUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UsersUseCase usersUseCase;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<String>> getUsersById(@PathVariable String id) {
        return usersUseCase.getUserById(id)
                .map(ResponseEntity::ok);
    }

    @PostMapping()
    public Mono<ResponseEntity<UserEntity>> createUser(@RequestBody UserDto user) {
        UserEntity userEntity = UserMapper.INTANCE.toEntity(user);
        return usersUseCase.createUser(userEntity)
                .map(ResponseEntity::ok);
    }

}