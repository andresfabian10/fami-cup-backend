package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @Size(max = 80) String username,
        @Size(max = 160) String fullName,
        @Email @Size(max = 160) String email,
        @Size(max = 40) String phone,
        @Size(min = 3, max = 80) String password,
        EstadoUsuario status) {
}
