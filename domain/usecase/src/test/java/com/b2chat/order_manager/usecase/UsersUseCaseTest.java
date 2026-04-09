package com.b2chat.order_manager.usecase;

import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.domain.users.gateway.UserGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsersUseCase Tests")
class UsersUseCaseTest {

    @Mock
    private UserGateway userGateway;

    private UsersUseCase usersUseCase;

    @BeforeEach
    void setUp() {
        usersUseCase = new UsersUseCase(userGateway);
    }

    private UserEntity buildUserRequest() {
        return UserEntity.builder()
                .name("Juan Pérez")
                .email("juan@test.com")
                .address("Calle 1")
                .build();
    }

    private UserEntity buildSavedUser() {
        return UserEntity.builder()
                .id(1L)
                .name("Juan Pérez")
                .email("juan@test.com")
                .address("Calle 1")
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
    }


    @Test
    @DisplayName("createUserUseCase - debe añadir timestamps y guardar el usuario")
    void createUserUseCase_shouldSetTimestampsAndSave() {
        UserEntity request = buildUserRequest();
        UserEntity saved = buildSavedUser();
        when(userGateway.createUser(any(UserEntity.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(usersUseCase.createUserUseCase(request))
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(1L);
                    assertThat(user.getName()).isEqualTo("Juan Pérez");
                    assertThat(user.getEmail()).isEqualTo("juan@test.com");
                })
                .verifyComplete();

        verify(userGateway).createUser(any(UserEntity.class));
    }

    @Test
    @DisplayName("createUserUseCase - debe enviar el usuario con createdAt y updatedAt asignados al gateway")
    void createUserUseCase_shouldPassUserWithTimestampsToGateway() {
        UserEntity request = buildUserRequest();
        when(userGateway.createUser(any(UserEntity.class)))
                .thenAnswer(inv -> {
                    UserEntity arg = inv.getArgument(0);
                    assertThat(arg.getCreatedAt()).isNotNull();
                    assertThat(arg.getUpdatedAt()).isNotNull();
                    return Mono.just(arg.toBuilder().id(1L).build());
                });

        StepVerifier.create(usersUseCase.createUserUseCase(request))
                .assertNext(user -> {
                    assertThat(user.getCreatedAt()).isNotNull();
                    assertThat(user.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createUserUseCase - createdAt y updatedAt deben ser iguales al momento de la creación")
    void createUserUseCase_shouldHaveEqualCreatedAtAndUpdatedAt() {
        UserEntity request = buildUserRequest();
        when(userGateway.createUser(any(UserEntity.class)))
                .thenAnswer(inv -> Mono.just(((UserEntity) inv.getArgument(0)).toBuilder().id(1L).build()));

        StepVerifier.create(usersUseCase.createUserUseCase(request))
                .assertNext(user ->
                        assertThat(user.getCreatedAt()).isEqualToIgnoringNanos(user.getUpdatedAt())
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("createUserUseCase - debe propagar el error cuando el gateway falla")
    void createUserUseCase_shouldPropagateError_whenGatewayFails() {
        when(userGateway.createUser(any(UserEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("Error al crear usuario")));

        StepVerifier.create(usersUseCase.createUserUseCase(buildUserRequest()))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && ex.getMessage().contains("Error al crear usuario"))
                .verify();
    }


    @Test
    @DisplayName("getUserByIdUseCase - debe retornar el usuario cuando existe")
    void getUserByIdUseCase_shouldReturnUser_whenExists() {
        UserEntity saved = buildSavedUser();
        when(userGateway.getUserById(1L)).thenReturn(Mono.just(saved));

        StepVerifier.create(usersUseCase.getUserByIdUseCase(1L))
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(1L);
                    assertThat(user.getName()).isEqualTo("Juan Pérez");
                    assertThat(user.getEmail()).isEqualTo("juan@test.com");
                })
                .verifyComplete();

        verify(userGateway).getUserById(1L);
    }

    @Test
    @DisplayName("getUserByIdUseCase - debe propagar ResourceNotFoundException cuando el usuario no existe")
    void getUserByIdUseCase_shouldPropagateError_whenUserNotFound() {
        when(userGateway.getUserById(eq(99L)))
                .thenReturn(Mono.error(new ResourceNotFoundException("Usuario", 99L)));

        StepVerifier.create(usersUseCase.getUserByIdUseCase(99L))
                .expectErrorMatches(ex -> ex instanceof ResourceNotFoundException
                        && ex.getMessage().contains("99"))
                .verify();

        verify(userGateway).getUserById(99L);
    }

    @Test
    @DisplayName("getUserByIdUseCase - debe propagar cualquier error genérico del gateway")
    void getUserByIdUseCase_shouldPropagateGenericError_whenGatewayFails() {
        when(userGateway.getUserById(eq(1L)))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        StepVerifier.create(usersUseCase.getUserByIdUseCase(1L))
                .expectErrorMatches(ex -> ex instanceof RuntimeException
                        && ex.getMessage().contains("Database connection error"))
                .verify();
    }
}

