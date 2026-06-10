package com.famicup.repositorio;

import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoApuestaColombia;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApuestaColombiaRepository extends JpaRepository<ApuestaColombia, UUID> {

    long countByUserAndMatch(Usuario user, Partido match);

    List<ApuestaColombia> findByUserAndMatch(Usuario user, Partido match);

    Optional<ApuestaColombia> findFirstByUserAndMatchAndPrincipalGlobalPredictionTrue(Usuario user, Partido match);

    @Modifying(flushAutomatically = true)
    @Query("""
            update ApuestaColombia bet
            set bet.principalGlobalPrediction = false
            where bet.user = :user
              and bet.match = :match
              and bet.principalGlobalPrediction = true
            """)
    int clearPrincipalForUserAndMatch(@Param("user") Usuario user, @Param("match") Partido match);

    List<ApuestaColombia> findByUserOrderByRegisteredAtDesc(Usuario user);

    List<ApuestaColombia> findAllByOrderByRegisteredAtDesc();

    List<ApuestaColombia> findByMatchAndStatus(Partido match, EstadoApuestaColombia status);

    List<ApuestaColombia> findByMatchInOrderByRegisteredAtDesc(Collection<Partido> matches);
}
