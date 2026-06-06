package com.famicup.repositorio;

import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoApuestaColombia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApuestaColombiaRepository extends JpaRepository<ApuestaColombia, UUID> {

    long countByUserAndMatch(Usuario user, Partido match);

    List<ApuestaColombia> findByUserAndMatch(Usuario user, Partido match);

    Optional<ApuestaColombia> findFirstByUserAndMatchAndPrincipalGlobalPredictionTrue(Usuario user, Partido match);

    List<ApuestaColombia> findByUserOrderByRegisteredAtDesc(Usuario user);

    List<ApuestaColombia> findByMatchAndStatus(Partido match, EstadoApuestaColombia status);
}
