package com.famicup.repositorio;

import com.famicup.modelo.entidad.Competicion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompeticionRepository extends JpaRepository<Competicion, Long> {

    Optional<Competicion> findByApiFootballLeagueId(Integer apiFootballLeagueId);
}
