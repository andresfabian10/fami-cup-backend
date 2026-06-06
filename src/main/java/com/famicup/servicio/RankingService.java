package com.famicup.servicio;

import com.famicup.modelo.dto.RankingResponse;
import com.famicup.modelo.entidad.PuntosRanking;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPronostico;
import com.famicup.modelo.enumeracion.EstadoCampeonMundial;
import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.repositorio.PronosticoGlobalRepository;
import com.famicup.repositorio.PronosticoCampeonMundialRepository;
import com.famicup.repositorio.PuntosRankingRepository;
import com.famicup.repositorio.UsuarioRepository;
import com.famicup.util.InicialesUtil;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingService {

    private final PuntosRankingRepository rankingRepository;
    private final PronosticoGlobalRepository predictionRepository;
    private final PronosticoCampeonMundialRepository championPredictionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PartidoService partidoService;

    public RankingService(
            PuntosRankingRepository rankingRepository,
            PronosticoGlobalRepository predictionRepository,
            PronosticoCampeonMundialRepository championPredictionRepository,
            UsuarioRepository usuarioRepository,
            PartidoService partidoService) {
        this.rankingRepository = rankingRepository;
        this.predictionRepository = predictionRepository;
        this.championPredictionRepository = championPredictionRepository;
        this.usuarioRepository = usuarioRepository;
        this.partidoService = partidoService;
    }

    @Cacheable("ranking")
    @Transactional(readOnly = true)
    public List<RankingResponse> getRanking(UUID currentUserId) {
        List<PuntosRanking> rows = rankingRepository.findAllByOrderByTotalPointsDescExactHitsDescWinnerHitsDescColombiaPointsDesc();
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setTotalPoints(rows.get(i).getTotalPoints() == null ? 0 : rows.get(i).getTotalPoints());
        }
        return buildRanking(rows, currentUserId);
    }

    @Transactional(readOnly = true)
    public int positionOf(UUID userId) {
        List<RankingResponse> ranking = getRanking(userId);
        return ranking.stream()
                .filter(row -> row.userId().equals(userId))
                .map(RankingResponse::position)
                .findFirst()
                .orElse(0);
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public void recalculateAll() {
        List<Usuario> players = usuarioRepository.findByRoleOrderByFullNameAsc(RolUsuario.PLAYER);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (Usuario player : players) {
            PuntosRanking ranking = rankingRepository.findById(player.getId()).orElseGet(() -> {
                PuntosRanking created = new PuntosRanking();
                created.setUser(player);
                return created;
            });

            var predictions = predictionRepository.findByUserOrderByRegisteredAtDesc(player).stream()
                    .filter(prediction -> prediction.getStatus() == EstadoPronostico.EVALUATED)
                    .toList();

            int total = predictions.stream().mapToInt(prediction -> prediction.getPoints() == null ? 0 : prediction.getPoints()).sum();
            total += championPredictionRepository.findByUser(player)
                    .filter(prediction -> prediction.getStatus() == EstadoCampeonMundial.EVALUATED)
                    .map(prediction -> prediction.getPoints() == null ? 0 : prediction.getPoints())
                    .orElse(0);
            int exact = (int) predictions.stream().filter(prediction -> prediction.isExactHit()).count();
            int winners = (int) predictions.stream().filter(prediction -> prediction.isWinnerHit()).count();
            int colombiaPoints = predictions.stream()
                    .filter(prediction -> partidoService.isColombiaMatch(prediction.getMatch()))
                    .mapToInt(prediction -> prediction.getPoints() == null ? 0 : prediction.getPoints())
                    .sum();

            ranking.setTotalPoints(total);
            ranking.setExactHits(exact);
            ranking.setWinnerHits(winners);
            ranking.setColombiaPoints(colombiaPoints);
            ranking.setPredictedMatches(predictions.size());
            ranking.setUpdatedAt(now);
            rankingRepository.save(ranking);
        }
    }

    private List<RankingResponse> buildRanking(List<PuntosRanking> rows, UUID currentUserId) {
        return java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(index -> toResponse(rows.get(index), index + 1, currentUserId))
                .toList();
    }

    private RankingResponse toResponse(PuntosRanking ranking, int position, UUID currentUserId) {
        Usuario user = ranking.getUser();
        return new RankingResponse(
                user.getId(),
                position,
                user.getFullName(),
                user.getUsername(),
                InicialesUtil.fromName(user.getFullName()),
                ranking.getTotalPoints(),
                ranking.getExactHits(),
                ranking.getWinnerHits(),
                ranking.getColombiaPoints(),
                currentUserId != null && currentUserId.equals(user.getId()));
    }
}
