package com.famicup.repositorio;

import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PronosticoGlobalRepository extends JpaRepository<PronosticoGlobal, UUID> {

    Optional<PronosticoGlobal> findByUserAndMatch(Usuario user, Partido match);

    List<PronosticoGlobal> findByUserAndMatchIn(Usuario user, Collection<Partido> matches);

    List<PronosticoGlobal> findByUserOrderByRegisteredAtDesc(Usuario user);

    List<PronosticoGlobal> findAllByOrderByRegisteredAtDesc();

    List<PronosticoGlobal> findByMatch(Partido match);
}
