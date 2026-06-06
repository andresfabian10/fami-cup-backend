package com.famicup.repositorio;

import com.famicup.modelo.entidad.PronosticoCampeonMundial;
import com.famicup.modelo.entidad.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PronosticoCampeonMundialRepository extends JpaRepository<PronosticoCampeonMundial, UUID> {

    Optional<PronosticoCampeonMundial> findByUser(Usuario user);

    List<PronosticoCampeonMundial> findAllByOrderByUpdatedAtDesc();
}
