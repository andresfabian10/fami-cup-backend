package com.famicup.repositorio;

import com.famicup.modelo.entidad.ResultadoPartido;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoPartidoRepository extends JpaRepository<ResultadoPartido, UUID> {

    Optional<ResultadoPartido> findByMatchId(Long matchId);
}
