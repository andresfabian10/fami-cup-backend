package com.famicup.repositorio;

import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRole(RolUsuario role);

    long countByRole(RolUsuario role);

    long countByRoleAndStatus(RolUsuario role, com.famicup.modelo.enumeracion.EstadoUsuario status);

    List<Usuario> findByRoleOrderByFullNameAsc(RolUsuario role);
}
