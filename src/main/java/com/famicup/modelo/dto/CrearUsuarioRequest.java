package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.modelo.validacion.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank(message = PasswordPolicy.REQUIRED_MESSAGE)
        @Size(min = PasswordPolicy.MIN_PASSWORD_LENGTH, message = PasswordPolicy.MIN_LENGTH_MESSAGE)
        @Size(max = PasswordPolicy.MAX_PASSWORD_LENGTH, message = PasswordPolicy.MAX_LENGTH_MESSAGE)
        String password,
        @NotBlank @Size(max = 160) String fullName,
        @Email @Size(max = 160) String email,
        @Size(max = 40) String phone,
        @NotNull RolUsuario role,
        boolean hasPaidGlobalRegistration) {
}
