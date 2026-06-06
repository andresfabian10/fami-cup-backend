package com.famicup.modelo.mapper;

import com.famicup.modelo.dto.UsuarioResponse;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario user) {
        return new UsuarioResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getRole() == RolUsuario.ADMIN,
                user.getRole() == RolUsuario.PLAYER,
                user.getLastLoginAt(),
                user.isMustChangePassword(),
                user.getPasswordChangedAt());
    }
}
