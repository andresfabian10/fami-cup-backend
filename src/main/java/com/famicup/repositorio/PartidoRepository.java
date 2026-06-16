package com.famicup.repositorio;

import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.enumeracion.EstadoPartido;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

    List<Partido> findTop20ByKickoffAtUtcAfterOrderByKickoffAtUtcAsc(OffsetDateTime from);

    List<Partido> findByStatusInAndKickoffAtUtcAfterOrderByKickoffAtUtcAsc(Collection<EstadoPartido> statuses, OffsetDateTime from);

    @Query("""
            select p from Partido p
            where p.status = :liveStatus
               or p.kickoffAtUtc between :from and :to
            order by p.kickoffAtUtc asc
            """)
    List<Partido> findResultSyncCandidates(
            @Param("liveStatus") EstadoPartido liveStatus,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("""
            select p from Partido p
            join fetch p.homeTeam
            join fetch p.awayTeam
            order by p.kickoffAtUtc asc
            """)
    List<Partido> findAllWithTeamsOrderByKickoffAtUtcAsc();

    @Query("""
            select p from Partido p
            join fetch p.homeTeam h
            join fetch p.awayTeam a
            where (h.fifaCode = 'COL' or a.fifaCode = 'COL' or h.apiFootballId = :colombiaTeamId or a.apiFootballId = :colombiaTeamId)
              and p.kickoffAtUtc >= :from
            order by p.kickoffAtUtc asc
            """)
    List<Partido> findUpcomingColombiaMatches(@Param("colombiaTeamId") Integer colombiaTeamId, @Param("from") OffsetDateTime from);

    @Query("""
            select p from Partido p
            join fetch p.homeTeam
            join fetch p.awayTeam
            where p.kickoffAtUtc >= :from
            order by p.kickoffAtUtc asc
            """)
    List<Partido> findUpcomingWithTeams(@Param("from") OffsetDateTime from);

    @Query("""
            select p from Partido p
            join fetch p.homeTeam
            join fetch p.awayTeam
            where p.kickoffAtUtc >= :from
              and p.kickoffAtUtc < :to
            order by p.kickoffAtUtc asc
            """)
    List<Partido> findByKickoffAtUtcBetweenWithTeams(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("""
            select p from Partido p
            where (:from is null or p.kickoffAtUtc >= :from)
              and (:to is null or p.kickoffAtUtc < :to)
            order by p.kickoffAtUtc asc
            """)
    List<Partido> findForScoringRange(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
