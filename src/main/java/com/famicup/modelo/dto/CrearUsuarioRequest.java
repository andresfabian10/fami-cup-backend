package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 3, max = 80) String password,
        @NotBlank @Size(max = 160) String fullName,
        @Email @Size(max = 160) String email,
        @Size(max = 40) String phone,
        @NotNull RolUsuario role,
        boolean hasPaidGlobalRegistration) {
}
