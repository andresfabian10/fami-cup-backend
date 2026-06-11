package com.famicup.modelo.dto;

import com.famicup.modelo.validacion.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank(message = PasswordPolicy.REQUIRED_MESSAGE)
        @Size(min = PasswordPolicy.MIN_PASSWORD_LENGTH, message = PasswordPolicy.MIN_LENGTH_MESSAGE)
        @Size(max = PasswordPolicy.MAX_PASSWORD_LENGTH, message = PasswordPolicy.MAX_LENGTH_MESSAGE)
        String newPassword) {
}
