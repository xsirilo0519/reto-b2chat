package com.b2chat.order_manager.repository.user.adapter;

import com.b2chat.order_manager.domain.exception.DuplicateEmailException;
import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.repository.user.data.UserData;
import com.b2chat.order_manager.repository.user.repository.UserDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAdapter Tests")
class UserAdapterTest {

    @Mock
    private UserDataRepository userDataRepository;

    private UserAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserAdapter(userDataRepository);
    }


    private UserData buildUserData(Long id) {
        UserData data = new UserData();
        data.setId(id);
        data.setName("Juan Pérez");
        data.setEmail("juan@example.com");
        data.setAddress("Calle 123, Bogotá");
        data.setCreatedAt(LocalDateTime.of(2025, 1, 10, 8, 0));
        data.setUpdatedAt(LocalDateTime.of(2025, 1, 10, 8, 0));
        return data;
    }

    private UserEntity buildUserEntity() {
        return UserEntity.builder()
                .name("Juan Pérez")
                .email("juan@example.com")
                .address("Calle 123, Bogotá")
                .createdAt(LocalDateTime.of(2025, 1, 10, 8, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 10, 8, 0))
                .build();
    }


    @Test
    @DisplayName("createUser - debe guardar y retornar el usuario creado")
    void createUser_shouldSaveAndReturnUser() {
        UserData savedData = buildUserData(1L);
        when(userDataRepository.save(any(UserData.class))).thenReturn(Mono.just(savedData));

        StepVerifier.create(adapter.createUser(buildUserEntity()))
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(1L);
                    assertThat(user.getName()).isEqualTo("Juan Pérez");
                    assertThat(user.getEmail()).isEqualTo("juan@example.com");
                    assertThat(user.getAddress()).isEqualTo("Calle 123, Bogotá");
                })
                .verifyComplete();

        verify(userDataRepository).save(any(UserData.class));
    }

    @Test
    @DisplayName("createUser - debe lanzar DuplicateEmailException cuando el email ya está registrado")
    void createUser_shouldThrowDuplicateEmailException_whenEmailAlreadyExists() {
        UserEntity userEntity = buildUserEntity();
        when(userDataRepository.save(any(UserData.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("unique constraint violation: email")));

        StepVerifier.create(adapter.createUser(userEntity))
                .expectErrorMatches(ex ->
                        ex instanceof DuplicateEmailException &&
                        ex.getMessage().contains("juan@example.com"))
                .verify();
    }

    @Test
    @DisplayName("createUser - no debe capturar excepciones distintas a DataIntegrityViolationException")
    void createUser_shouldPropagateOtherExceptions_whenNotDataIntegrityViolation() {
        when(userDataRepository.save(any(UserData.class)))
                .thenReturn(Mono.error(new RuntimeException("Conexión DB perdida")));

        StepVerifier.create(adapter.createUser(buildUserEntity()))
                .expectErrorMatches(ex -> ex instanceof RuntimeException &&
                        ex.getMessage().contains("Conexión DB perdida"))
                .verify();
    }


    @Test
    @DisplayName("getUserById - debe retornar el usuario cuando existe")
    void getUserById_shouldReturnUser_whenFound() {
        when(userDataRepository.findById(1L)).thenReturn(Mono.just(buildUserData(1L)));

        StepVerifier.create(adapter.getUserById(1L))
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(1L);
                    assertThat(user.getName()).isEqualTo("Juan Pérez");
                    assertThat(user.getEmail()).isEqualTo("juan@example.com");
                    assertThat(user.getAddress()).isEqualTo("Calle 123, Bogotá");
                })
                .verifyComplete();

        verify(userDataRepository).findById(1L);
    }

    @Test
    @DisplayName("getUserById - debe lanzar ResourceNotFoundException cuando el usuario no existe")
    void getUserById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(userDataRepository.findById(eq(99L))).thenReturn(Mono.empty());

        StepVerifier.create(adapter.getUserById(99L))
                .expectErrorMatches(ex ->
                        ex instanceof ResourceNotFoundException &&
                        ex.getMessage().contains("99"))
                .verify();
    }

    @Test
    @DisplayName("getUserById - el mensaje de error debe indicar el ID del usuario no encontrado")
    void getUserById_shouldIncludeIdInErrorMessage_whenNotFound() {
        when(userDataRepository.findById(eq(42L))).thenReturn(Mono.empty());

        StepVerifier.create(adapter.getUserById(42L))
                .expectErrorMatches(ex ->
                        ex instanceof ResourceNotFoundException &&
                        ex.getMessage().equals("Usuario no encontrado con id: 42"))
                .verify();
    }
}

