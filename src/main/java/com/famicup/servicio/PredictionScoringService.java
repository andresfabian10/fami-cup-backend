package com.famicup.servicio;

import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.dto.ScoringRecalculationResponse;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.ResultadoPartido;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPronostico;
import com.famicup.modelo.enumeracion.GanadorPartido;
import com.famicup.repositorio.PartidoRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import com.famicup.repositorio.ResultadoPartidoRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PredictionScoringService {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    private static final String STATUS_CALCULATED = "CALCULATED";
    private static final String STATUS_PENDING_RESULT = "PENDING_RESULT";
    private static final String REASON_EXACT = "Marcador exacto";
    private static final String REASON_WINNER = "Ganador acertado";
    private static final String REASON_DRAW = "Empate acertado";
    private static final String REASON_INCORRECT = "Pronostico incorrecto";
    private static final String REASON_PENDING_RESULT = "Pendiente de resultado";

    private final PronosticoGlobalRepository predictionRepository;
    private final ResultadoPartidoRepository resultRepository;
    private final PartidoRepository partidoRepository;
    private final BettingParametersService parametersService;
    private final RankingService rankingService;
    private final AuditService auditService;

    public PredictionScoringService(
            PronosticoGlobalRepository predictionRepository,
            ResultadoPartidoRepository resultRepository,
            PartidoRepository partidoRepository,
            BettingParametersService parametersService,
            RankingService rankingService,
            AuditService auditService) {
        this.predictionRepository = predictionRepository;
        this.resultRepository = resultRepository;
        this.partidoRepository = partidoRepository;
        this.parametersService = parametersService;
        this.rankingService = rankingService;
        this.auditService = auditService;
    }

    public ScoringDecision calculate(
            Integer homeScoreReal,
            Integer awayScoreReal,
            Integer homeScorePredicted,
            Integer awayScorePredicted) {
        if (homeScoreReal == null || awayScoreReal == null || homeScorePredicted == null || awayScorePredicted == null) {
            return new ScoringDecision(0, REASON_PENDING_RESULT, STATUS_PENDING_RESULT, false, false);
        }

        boolean exact = homeScoreReal.equals(homeScorePredicted) && awayScoreReal.equals(awayScorePredicted);
        GanadorPartido realWinner = winnerOf(homeScoreReal, awayScoreReal);
        GanadorPartido predictedWinner = winnerOf(homeScorePredicted, awayScorePredicted);
        boolean trendHit = realWinner == predictedWinner;

        if (exact) {
            return new ScoringDecision(parametersService.getParameters().exactPoints(), REASON_EXACT, STATUS_CALCULATED, true, false);
        }
        if (trendHit && realWinner == GanadorPartido.DRAW) {
            return new ScoringDecision(parametersService.getParameters().winnerPoints(), REASON_DRAW, STATUS_CALCULATED, false, true);
        }
        if (trendHit) {
            return new ScoringDecision(parametersService.getParameters().winnerPoints(), REASON_WINNER, STATUS_CALCULATED, false, true);
        }
        return new ScoringDecision(0, REASON_INCORRECT, STATUS_CALCULATED, false, false);
    }

    @Transactional
    public boolean evaluateIfResultExists(PronosticoGlobal prediction) {
        ResultadoPartido result = resultRepository.findByMatchId(prediction.getMatch().getId()).orElse(null);
        if (result == null || prediction.getStatus() == EstadoPronostico.ANNULLED) {
            return false;
        }
        applyDecision(prediction, result, OffsetDateTime.now(ZoneOffset.UTC));
        return true;
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public int repairUserPredictionsWithResults(Usuario user) {
        int repaired = 0;
        for (PronosticoGlobal prediction : predictionRepository.findByUserOrderByRegisteredAtDesc(user)) {
            if (prediction.getStatus() == EstadoPronostico.EVALUATED || prediction.getStatus() == EstadoPronostico.ANNULLED) {
                continue;
            }
            if (evaluateIfResultExists(prediction)) {
                repaired++;
            }
        }
        if (repaired > 0) {
            rankingService.recalculateAll();
            auditService.record(
                    user,
                    "SCORING_AUTO_REPAIR_PLAYER_VIEW",
                    "GLOBAL_PREDICTION",
                    user.getId().toString(),
                    "Reparo pronosticos con resultado existente al consultar vista de jugador",
                    "Pronosticos reparados: " + repaired);
        }
        return repaired;
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public ScoringRecalculationResponse recalculateMatch(Partido match, Usuario actor, boolean dryRun, String action) {
        ScoringAccumulator accumulator = new ScoringAccumulator(dryRun, match.getId(), null, null);
        processMatches(List.of(match), dryRun, accumulator);
        if (!dryRun) {
            rankingService.recalculateAll();
        }
        auditRecalculation(actor, action, accumulator.toResponse());
        return accumulator.toResponse();
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public ScoringRecalculationResponse recalculate(Usuario actor, Long matchId, LocalDate from, LocalDate to, boolean dryRun) {
        if (matchId != null && (from != null || to != null)) {
            throw new ReglaNegocioException("Usa matchId o rango de fechas, no ambos filtros al tiempo.");
        }

        OffsetDateTime fromUtc = startOfDayUtc(from);
        OffsetDateTime toUtc = endExclusiveUtc(to);
        List<Partido> matches = matchId == null
                ? partidoRepository.findForScoringRange(fromUtc, toUtc)
                : List.of(partidoRepository.findById(matchId)
                        .orElseThrow(() -> new ReglaNegocioException("Partido no encontrado para recalculo.")));

        ScoringAccumulator accumulator = new ScoringAccumulator(dryRun, matchId, fromUtc, toUtc);
        processMatches(matches, dryRun, accumulator);
        if (!dryRun) {
            rankingService.recalculateAll();
        }
        auditRecalculation(actor, dryRun ? "SCORING_RECALCULATE_DRY_RUN" : "SCORING_RECALCULATE", accumulator.toResponse());
        return accumulator.toResponse();
    }

    private void processMatches(Collection<Partido> matches, boolean dryRun, ScoringAccumulator accumulator) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (Partido match : matches) {
            ResultadoPartido result = resultRepository.findByMatchId(match.getId()).orElse(null);
            if (result == null) {
                accumulator.matchesSkipped++;
                continue;
            }
            accumulator.matchesProcessed++;
            List<PronosticoGlobal> predictions = predictionRepository.findByMatch(match);
            for (PronosticoGlobal prediction : predictions) {
                try {
                    if (prediction.getStatus() == EstadoPronostico.ANNULLED) {
                        accumulator.predictionsSkipped++;
                        continue;
                    }
                    accumulator.predictionsProcessed++;
                    ScoringDecision decision = calculate(
                            result.getHomeGoals90(),
                            result.getAwayGoals90(),
                            prediction.getPredictedHomeGoals(),
                            prediction.getPredictedAwayGoals());
                    boolean changed = predictionChanged(prediction, decision);
                    if (changed) {
                        accumulator.predictionsUpdated++;
                        if (!Objects.equals(prediction.getPoints(), decision.points())) {
                            accumulator.pointsChanged++;
                        }
                    } else {
                        accumulator.predictionsUnchanged++;
                    }
                    if (!dryRun) {
                        applyDecision(prediction, decision, now);
                    }
                } catch (RuntimeException exception) {
                    accumulator.errors.add("Partido " + match.getId() + ", pronostico " + prediction.getId() + ": " + exception.getMessage());
                }
            }
        }
    }

    private boolean predictionChanged(PronosticoGlobal prediction, ScoringDecision decision) {
        return prediction.getStatus() != EstadoPronostico.EVALUATED
                || !Objects.equals(prediction.getPoints(), decision.points())
                || prediction.isExactHit() != decision.exactHit()
                || prediction.isWinnerHit() != decision.winnerHit();
    }

    private void applyDecision(PronosticoGlobal prediction, ResultadoPartido result, OffsetDateTime now) {
        ScoringDecision decision = calculate(
                result.getHomeGoals90(),
                result.getAwayGoals90(),
                prediction.getPredictedHomeGoals(),
                prediction.getPredictedAwayGoals());
        applyDecision(prediction, decision, now);
    }

    private void applyDecision(PronosticoGlobal prediction, ScoringDecision decision, OffsetDateTime now) {
        prediction.setExactHit(decision.exactHit());
        prediction.setWinnerHit(decision.winnerHit());
        prediction.setPoints(decision.points());
        prediction.setStatus(EstadoPronostico.EVALUATED);
        prediction.setEvaluatedAt(now);
    }

    private GanadorPartido winnerOf(int homeGoals, int awayGoals) {
        if (homeGoals > awayGoals) {
            return GanadorPartido.HOME;
        }
        if (awayGoals > homeGoals) {
            return GanadorPartido.AWAY;
        }
        return GanadorPartido.DRAW;
    }

    private OffsetDateTime startOfDayUtc(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(BOGOTA_ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    private OffsetDateTime endExclusiveUtc(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.plusDays(1).atStartOfDay(BOGOTA_ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    private void auditRecalculation(Usuario actor, String action, ScoringRecalculationResponse response) {
        auditService.record(
                actor,
                action,
                "GLOBAL_PREDICTION",
                response.matchId() == null ? "range" : response.matchId().toString(),
                "Recalculo puntos. dryRun=" + response.dryRun() + ", from=" + response.from() + ", to=" + response.to(),
                "Partidos=" + response.matchesProcessed()
                        + ", pronosticos=" + response.predictionsProcessed()
                        + ", actualizados=" + response.predictionsUpdated()
                        + ", errores=" + response.errorsFound());
    }

    public record ScoringDecision(
            int points,
            String reason,
            String status,
            boolean exactHit,
            boolean winnerHit) {
    }

    private static final class ScoringAccumulator {

        private final boolean dryRun;
        private final Long matchId;
        private final OffsetDateTime from;
        private final OffsetDateTime to;
        private int matchesProcessed;
        private int matchesSkipped;
        private int predictionsProcessed;
        private int predictionsUpdated;
        private int predictionsUnchanged;
        private int predictionsSkipped;
        private int pointsChanged;
        private final List<String> errors = new ArrayList<>();

        private ScoringAccumulator(boolean dryRun, Long matchId, OffsetDateTime from, OffsetDateTime to) {
            this.dryRun = dryRun;
            this.matchId = matchId;
            this.from = from;
            this.to = to;
        }

        private ScoringRecalculationResponse toResponse() {
            return new ScoringRecalculationResponse(
                    dryRun,
                    matchId,
                    from,
                    to,
                    matchesProcessed,
                    matchesSkipped,
                    predictionsProcessed,
                    predictionsUpdated,
                    predictionsUnchanged,
                    predictionsSkipped,
                    pointsChanged,
                    errors.size(),
                    List.copyOf(errors));
        }
    }
}
