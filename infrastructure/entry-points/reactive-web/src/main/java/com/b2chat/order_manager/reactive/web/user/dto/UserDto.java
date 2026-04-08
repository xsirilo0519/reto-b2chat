package com.b2chat.order_manager.reactive.web.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserDto {

    @NotBlank(message = "El nombre no puede estar vacío")
    @Pattern(
            regexp = "^[^0-9]+$",
            message = "El nombre no puede contener números"
    )
    private String name;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "El correo no tiene un formato válido")
    private String email;

    @NotBlank(message = "La dirección no puede estar vacía")
    private String address;
}
