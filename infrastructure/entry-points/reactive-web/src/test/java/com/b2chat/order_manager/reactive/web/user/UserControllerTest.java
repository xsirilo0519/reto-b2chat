package com.b2chat.order_manager.reactive.web.user;

import com.b2chat.order_manager.domain.exception.DuplicateEmailException;
import com.b2chat.order_manager.domain.exception.ResourceNotFoundException;
import com.b2chat.order_manager.domain.users.entity.UserEntity;
import com.b2chat.order_manager.reactive.web.exception.GlobalExceptionHandler;
import com.b2chat.order_manager.reactive.web.user.dto.UserDto;
import com.b2chat.order_manager.usecase.UsersUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UsersUseCase usersUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(usersUseCase);
        webTestClient = WebTestClient.bindToController(userController)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    @Test
    @DisplayName("GET /users/{id} - debe retornar 200 OK con el usuario cuando existe")
    void getUserById_shouldReturnUser_whenFound() {
        UserEntity userEntity = UserEntity.builder()
                .id(1L)
                .name("Juan Pérez")
                .email("juan.perez@example.com")
                .address("Calle 123, Bogotá")
                .createdAt(LocalDateTime.of(2025, 1, 10, 8, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 10, 8, 0))
                .build();

        when(usersUseCase.getUserByIdUseCase(1L)).thenReturn(Mono.just(userEntity));

        webTestClient.get()
                .uri("/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Juan Pérez")
                .jsonPath("$.email").isEqualTo("juan.perez@example.com")
                .jsonPath("$.address").isEqualTo("Calle 123, Bogotá");

        verify(usersUseCase).getUserByIdUseCase(1L);
    }

    @Test
    @DisplayName("GET /users/{id} - debe verificar todos los campos del response")
    void getUserById_shouldReturnAllFields_whenFound() {
        UserEntity userEntity = UserEntity.builder()
                .id(5L)
                .name("María López")
                .email("maria.lopez@example.com")
                .address("Av. Siempreviva 742")
                .createdAt(LocalDateTime.of(2025, 2, 15, 12, 30))
                .updatedAt(LocalDateTime.of(2025, 3, 20, 14, 0))
                .build();

        when(usersUseCase.getUserByIdUseCase(5L)).thenReturn(Mono.just(userEntity));

        webTestClient.get()
                .uri("/users/5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.name").isEqualTo("María López")
                .jsonPath("$.email").isEqualTo("maria.lopez@example.com")
                .jsonPath("$.address").isEqualTo("Av. Siempreviva 742");

        verify(usersUseCase).getUserByIdUseCase(5L);
    }


    @Test
    @DisplayName("POST /users - debe retornar 201 CREATED cuando el usuario es válido")
    void createUser_shouldReturnCreated_whenUserIsValid() {
        UserDto userDto = UserDto.builder()
                .name("Carlos Ruiz")
                .email("carlos.ruiz@example.com")
                .address("Carrera 45, Medellín")
                .build();

        UserEntity createdUser = UserEntity.builder()
                .id(1L)
                .name("Carlos Ruiz")
                .email("carlos.ruiz@example.com")
                .address("Carrera 45, Medellín")
                .createdAt(LocalDateTime.of(2025, 4, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2025, 4, 1, 9, 0))
                .build();

        when(usersUseCase.createUserUseCase(any(UserEntity.class)))
                .thenReturn(Mono.just(createdUser));

        webTestClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Carlos Ruiz")
                .jsonPath("$.email").isEqualTo("carlos.ruiz@example.com")
                .jsonPath("$.address").isEqualTo("Carrera 45, Medellín");

        verify(usersUseCase).createUserUseCase(any(UserEntity.class));
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 BAD REQUEST cuando el nombre está vacío")
    void createUser_shouldReturnBadRequest_whenNameIsBlank() {
        UserDto userDto = UserDto.builder()
                .name("")
                .email("valido@example.com")
                .address("Calle 10")
                .build();

        webTestClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 BAD REQUEST cuando el nombre contiene números")
    void createUser_shouldReturnBadRequest_whenNameContainsNumbers() {
        UserDto userDto = UserDto.builder()
                .name("Carlos123")
                .email("carlos@example.com")
                .address("Calle 10")
                .build();

        webTestClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 BAD REQUEST cuando el email tiene formato inválido")
    void createUser_shouldReturnBadRequest_whenEmailIsInvalid() {
        UserDto userDto = UserDto.builder()
                .name("Ana Torres")
                .email("no-es-un-email")
                .address("Calle 10")
                .build();

        webTestClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 BAD REQUEST cuando el email está vacío")
    void createUser_shouldReturnBadRequest_whenEmailIsBlank() {
        UserDto userDto = UserDto.builder()
                .name("Ana Torres")
                .email("")
                .address("Calle 10")
                .build();

        webTestClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /users - debe retornar 400 BAD REQUEST cuando la dirección está vacía")
    void createUser_shouldReturnBadRequest_whenAddressIsBlank() {
        UserDto userDto = UserDto.builder()
                .name("Ana Torres")
                .email("ana@example.com")
                .address("")
                .build();

        webTestClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isBadRequest();
    }


    @Test
    @DisplayName("POST /users - debe retornar 409 cuando el email ya existe (DuplicateEmailException)")
    void createUser_shouldReturn409_whenEmailAlreadyExists() {
        UserDto userDto = UserDto.builder()
                .name("Carlos Ruiz")
                .email("carlos.ruiz@example.com")
                .address("Carrera 45, Medellín")
                .build();

        when(usersUseCase.createUserUseCase(any(UserEntity.class)))
                .thenReturn(Mono.error(new DuplicateEmailException("carlos.ruiz@example.com")));

        webTestClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.message").isEqualTo("El usuario ya está registrado: carlos.ruiz@example.com");
    }

    @Test
    @DisplayName("GET /users/{id} - debe retornar 404 cuando el usuario no existe (ResourceNotFoundException)")
    void getUserById_shouldReturn404_whenUserNotFound() {
        when(usersUseCase.getUserByIdUseCase(5L))
                .thenReturn(Mono.error(new ResourceNotFoundException("User", 5L)));

        webTestClient.get()
                .uri("/users/5")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("User no encontrado con id: 5");
    }
}



