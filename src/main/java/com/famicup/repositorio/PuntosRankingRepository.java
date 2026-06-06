package com.famicup.repositorio;

import com.famicup.modelo.entidad.PuntosRanking;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuntosRankingRepository extends JpaRepository<PuntosRanking, UUID> {

    List<PuntosRanking> findAllByOrderByTotalPointsDescExactHitsDescWinnerHitsDescColombiaPointsDesc();
}
