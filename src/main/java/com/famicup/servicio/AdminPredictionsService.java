package com.famicup.servicio;

import com.famicup.modelo.dto.AdminPredictionsResponse;
import com.famicup.modelo.dto.EquipoDto;
import com.famicup.modelo.dto.PartidoDto;
import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoCampeonMundial;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoApuestaColombia;
import com.famicup.modelo.enumeracion.EstadoCampeonMundial;
import com.famicup.modelo.enumeracion.EstadoPronostico;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.repositorio.ApuestaColombiaRepository;
import com.famicup.repositorio.PronosticoCampeonMundialRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPredictionsService {

    private static final String MODALITY_COLOMBIA = "COLOMBIA";
    private static final String MODALITY_GLOBAL = "GLOBAL";
    private static final String MODALITY_WORLD_CHAMPION = "WORLD_CHAMPION";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_REGISTERED = "REGISTERED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_WON = "WON";
    private static final String STATUS_LOST = "LOST";

    private final ApuestaColombiaRepository colombiaBetRepository;
    private final PronosticoGlobalRepository globalPredictionRepository;
    private final PronosticoCampeonMundialRepository championPredictionRepository;
    private final PartidoMapper partidoMapper;
    private final BettingParametersService parametersService;
    private final PredictionScoringService scoringService;

    public AdminPredictionsService(
            ApuestaColombiaRepository colombiaBetRepository,
            PronosticoGlobalRepository globalPredictionRepository,
            PronosticoCampeonMundialRepository championPredictionRepository,
            PartidoMapper partidoMapper,
            BettingParametersService parametersService,
            PredictionScoringService scoringService) {
        this.colombiaBetRepository = colombiaBetRepository;
        this.globalPredictionRepository = globalPredictionRepository;
        this.championPredictionRepository = championPredictionRepository;
        this.partidoMapper = partidoMapper;
        this.parametersService = parametersService;
        this.scoringService = scoringService;
    }

    @Transactional
    public AdminPredictionsResponse listPredictions(Usuario admin) {
        scoringService.repairPredictionsWithResults(admin, "SCORING_AUTO_REPAIR_ADMIN_PREDICTIONS_VIEW");

        ParametrosApuestasResponse parameters = parametersService.getParameters();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<ApuestaColombia> colombiaBets = colombiaBetRepository.findAllByOrderByRegisteredAtDesc();
        List<PronosticoGlobal> globalPredictions = globalPredictionRepository.findAllByOrderByRegisteredAtDesc();
        List<PronosticoCampeonMundial> championPredictions = championPredictionRepository.findAllByOrderByUpdatedAtDesc();
        List<AdminPredictionsResponse.Item> items = new ArrayList<>();

        colombiaBets.forEach(bet -> items.add(toColombiaItem(bet, parameters, now)));
        globalPredictions.forEach(prediction -> items.add(toGlobalItem(prediction, parameters, now)));
        championPredictions.forEach(prediction -> items.add(toChampionItem(prediction, parameters, now)));

        items.sort(Comparator
                .comparing(AdminPredictionsResponse.Item::updatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AdminPredictionsResponse.Item::registeredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());

        return new AdminPredictionsResponse(
                buildSummary(colombiaBets, globalPredictions, championPredictions, items),
                items);
    }

    private AdminPredictionsResponse.Item toColombiaItem(ApuestaColombia bet, ParametrosApuestasResponse parameters, OffsetDateTime now) {
        Partido match = bet.getMatch();
        PartidoDto matchDto = partidoMapper.toDto(match);
        String predictionLabel = scoreLabel(bet.getPredictedHomeGoals(), bet.getPredictedAwayGoals());
        return new AdminPredictionsResponse.Item(
                bet.getId(),
                MODALITY_COLOMBIA,
                toPlayer(bet.getUser()),
                matchDto,
                null,
                bet.getPredictedHomeGoals(),
                bet.getPredictedAwayGoals(),
                predictionLabel,
                resultLabel(matchDto),
                bet.isPrincipalGlobalPrediction(),
                bet.getStatus().name(),
                colombiaStatus(bet, parameters, now),
                0,
                bet.isPrincipalGlobalPrediction() ? parameters.exactPoints() : 0,
                null,
                null,
                bet.getRegisteredAt(),
                bet.getUpdatedAt());
    }

    private AdminPredictionsResponse.Item toGlobalItem(PronosticoGlobal prediction, ParametrosApuestasResponse parameters, OffsetDateTime now) {
        PartidoDto matchDto = partidoMapper.toDto(prediction.getMatch());
        String predictionLabel = scoreLabel(prediction.getPredictedHomeGoals(), prediction.getPredictedAwayGoals());
        PredictionScoringService.ScoringDecision decision = scoringDecision(prediction, matchDto);
        return new AdminPredictionsResponse.Item(
                prediction.getId(),
                MODALITY_GLOBAL,
                toPlayer(prediction.getUser()),
                matchDto,
                null,
                prediction.getPredictedHomeGoals(),
                prediction.getPredictedAwayGoals(),
                predictionLabel,
                resultLabel(matchDto),
                false,
                prediction.getStatus().name(),
                globalStatus(prediction, parameters, now),
                prediction.getPoints(),
                parameters.exactPoints(),
                decision.reason(),
                decision.status(),
                prediction.getRegisteredAt(),
                prediction.getUpdatedAt());
    }

    private AdminPredictionsResponse.Item toChampionItem(PronosticoCampeonMundial prediction, ParametrosApuestasResponse parameters, OffsetDateTime now) {
        EquipoDto team = partidoMapper.toTeamDto(prediction.getTeam());
        return new AdminPredictionsResponse.Item(
                prediction.getId(),
                MODALITY_WORLD_CHAMPION,
                toPlayer(prediction.getUser()),
                null,
                team,
                null,
                null,
                team.displayName(),
                null,
                false,
                championRawStatus(prediction, parameters, now),
                championStatus(prediction, parameters, now),
                prediction.getPoints(),
                parameters.worldChampionPoints(),
                null,
                null,
                prediction.getCreatedAt(),
                prediction.getUpdatedAt());
    }

    private AdminPredictionsResponse.Summary buildSummary(
            List<ApuestaColombia> colombiaBets,
            List<PronosticoGlobal> globalPredictions,
            List<PronosticoCampeonMundial> championPredictions,
            List<AdminPredictionsResponse.Item> items) {
        Set<UUID> playersWithPredictions = new HashSet<>();
        items.forEach(item -> playersWithPredictions.add(item.player().id()));
        long principalCount = colombiaBets.stream().filter(ApuestaColombia::isPrincipalGlobalPrediction).count();
        TopMatch topMatch = topMatch(colombiaBets, globalPredictions);

        return new AdminPredictionsResponse.Summary(
                items.size(),
                playersWithPredictions.size(),
                colombiaBets.size(),
                globalPredictions.size(),
                championPredictions.size(),
                Math.toIntExact(principalCount),
                topMatch.label(),
                topMatch.count());
    }

    private TopMatch topMatch(List<ApuestaColombia> colombiaBets, List<PronosticoGlobal> globalPredictions) {
        Map<Long, MatchCounter> counters = new HashMap<>();
        colombiaBets.forEach(bet -> increment(counters, bet.getMatch()));
        globalPredictions.forEach(prediction -> increment(counters, prediction.getMatch()));

        return counters.values().stream()
                .max(Comparator.comparingInt(MatchCounter::count))
                .map(counter -> new TopMatch(matchLabel(counter.match()), counter.count()))
                .orElse(new TopMatch("", 0));
    }

    private void increment(Map<Long, MatchCounter> counters, Partido match) {
        counters.compute(match.getId(), (ignored, current) -> current == null ? new MatchCounter(match, 1) : current.increment());
    }

    private AdminPredictionsResponse.Player toPlayer(Usuario user) {
        return new AdminPredictionsResponse.Player(user.getId(), user.getUsername(), user.getFullName());
    }

    private String colombiaStatus(ApuestaColombia bet, ParametrosApuestasResponse parameters, OffsetDateTime now) {
        if (bet.getStatus() == EstadoApuestaColombia.WON) {
            return STATUS_WON;
        }
        if (bet.getStatus() == EstadoApuestaColombia.LOST) {
            return STATUS_LOST;
        }
        if (bet.getStatus() == EstadoApuestaColombia.ANNULLED) {
            return STATUS_CLOSED;
        }
        if (bet.getStatus() == EstadoApuestaColombia.PENDING_PAYMENT) {
            return STATUS_PENDING;
        }
        return isMatchClosed(bet.getMatch(), parameters, now) ? STATUS_CLOSED : STATUS_REGISTERED;
    }

    private String globalStatus(PronosticoGlobal prediction, ParametrosApuestasResponse parameters, OffsetDateTime now) {
        if (prediction.getStatus() == EstadoPronostico.EVALUATED) {
            return prediction.getPoints() > 0 ? STATUS_WON : STATUS_LOST;
        }
        if (prediction.getStatus() == EstadoPronostico.ANNULLED) {
            return STATUS_CLOSED;
        }
        return isMatchClosed(prediction.getMatch(), parameters, now) ? STATUS_CLOSED : STATUS_REGISTERED;
    }

    private String championStatus(PronosticoCampeonMundial prediction, ParametrosApuestasResponse parameters, OffsetDateTime now) {
        if (prediction.getStatus() == EstadoCampeonMundial.EVALUATED) {
            return prediction.getPoints() > 0 ? STATUS_WON : STATUS_LOST;
        }
        return isChampionLocked(parameters, now) ? STATUS_CLOSED : STATUS_REGISTERED;
    }

    private String championRawStatus(PronosticoCampeonMundial prediction, ParametrosApuestasResponse parameters, OffsetDateTime now) {
        return isChampionLocked(parameters, now) && prediction.getStatus() == EstadoCampeonMundial.VALID
                ? EstadoCampeonMundial.LOCKED.name()
                : prediction.getStatus().name();
    }

    private boolean isMatchClosed(Partido match, ParametrosApuestasResponse parameters, OffsetDateTime now) {
        return !now.isBefore(match.getKickoffAtUtc().minusMinutes(parameters.closingMinutesBeforeMatch()));
    }

    private boolean isChampionLocked(ParametrosApuestasResponse parameters, OffsetDateTime now) {
        return parameters.worldChampionLockAt() != null && !now.isBefore(parameters.worldChampionLockAt());
    }

    private String scoreLabel(Integer homeGoals, Integer awayGoals) {
        return homeGoals + " - " + awayGoals;
    }

    private String resultLabel(PartidoDto match) {
        if (match == null || match.result() == null) {
            return null;
        }
        return scoreLabel(match.result().homeGoals90(), match.result().awayGoals90());
    }

    private PredictionScoringService.ScoringDecision scoringDecision(PronosticoGlobal prediction, PartidoDto match) {
        Integer homeScoreReal = match.result() == null ? null : match.result().homeGoals90();
        Integer awayScoreReal = match.result() == null ? null : match.result().awayGoals90();
        return scoringService.calculate(
                homeScoreReal,
                awayScoreReal,
                prediction.getPredictedHomeGoals(),
                prediction.getPredictedAwayGoals());
    }

    private String matchLabel(Partido match) {
        PartidoDto dto = partidoMapper.toDto(match);
        return dto.homeTeam().displayName() + " vs " + dto.awayTeam().displayName();
    }

    private record MatchCounter(Partido match, int count) {
        private MatchCounter increment() {
            return new MatchCounter(match, count + 1);
        }
    }

    private record TopMatch(String label, int count) {
    }
}
