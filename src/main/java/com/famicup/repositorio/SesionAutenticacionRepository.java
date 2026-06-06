package com.famicup.repositorio;

import com.famicup.modelo.entidad.SesionAutenticacion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionAutenticacionRepository extends JpaRepository<SesionAutenticacion, UUID> {

    Optional<SesionAutenticacion> findByRefreshTokenHash(String refreshTokenHash);
}
