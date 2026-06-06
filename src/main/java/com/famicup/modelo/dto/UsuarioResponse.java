package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoUsuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        String phone,
        RolUsuario role,
        EstadoUsuario status,
        boolean admin,
        boolean player,
        OffsetDateTime lastLoginAt,
        boolean mustChangePassword,
        OffsetDateTime passwordChangedAt) {
}
