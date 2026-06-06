package com.famicup.repositorio;

import com.famicup.modelo.entidad.Equipo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepository extends JpaRepository<Equipo, String> {

    Optional<Equipo> findByApiFootballId(Integer apiFootballId);

    List<Equipo> findAllByOrderByNameAsc();
}
