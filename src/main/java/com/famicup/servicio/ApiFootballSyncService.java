package com.famicup.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.famicup.cliente.ApiFootballClient;
import com.famicup.configuracion.ApiFootballProperties;
import com.famicup.modelo.dto.SyncResponse;
import com.famicup.modelo.entidad.ApiSyncLog;
import com.famicup.modelo.entidad.Competicion;
import com.famicup.modelo.entidad.Equipo;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.ResultadoPartido;
import com.famicup.modelo.enumeracion.EstadoPartido;
import com.famicup.modelo.enumeracion.EstadoSincronizacion;
import com.famicup.modelo.enumeracion.GanadorPartido;
import com.famicup.modelo.enumeracion.TipoSincronizacion;
import com.famicup.repositorio.ApiSyncLogRepository;
import com.famicup.repositorio.CompeticionRepository;
import com.famicup.repositorio.EquipoRepository;
import com.famicup.repositorio.PartidoRepository;
import com.famicup.repositorio.ResultadoPartidoRepository;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiFootballSyncService {

    private final ApiFootballClient apiFootballClient;
    private final ApiFootballProperties properties;
    private final EquipoRepository equipoRepository;
    private final CompeticionRepository competicionRepository;
    private final PartidoRepository partidoRepository;
    private final ResultadoPartidoRepository resultadoRepository;
    private final ApiSyncLogRepository syncLogRepository;
    private final GlobalPredictionService globalPredictionService;
    private final AuditService auditService;

    public ApiFootballSyncService(
            ApiFootballClient apiFootballClient,
            ApiFootballProperties properties,
            EquipoRepository equipoRepository,
            CompeticionRepository competicionRepository,
            PartidoRepository partidoRepository,
            ResultadoPartidoRepository resultadoRepository,
            ApiSyncLogRepository syncLogRepository,
            GlobalPredictionService globalPredictionService,
            AuditService auditService) {
        this.apiFootballClient = apiFootballClient;
        this.properties = properties;
        this.equipoRepository = equipoRepository;
        this.competicionRepository = competicionRepository;
        this.partidoRepository = partidoRepository;
        this.resultadoRepository = resultadoRepository;
        this.syncLogRepository = syncLogRepository;
        this.globalPredictionService = globalPredictionService;
        this.auditService = auditService;
    }

    @CacheEvict(value = {"teams", "upcomingMatches", "ranking"}, allEntries = true)
    @Transactional
    public SyncResponse syncFixtures() {
        OffsetDateTime started = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            JsonNode payload = apiFootballClient.fixturesByLeagueAndSeason();
            int processed = processResponse(payload, false);
            return saveLog(TipoSincronizacion.FIXTURES, EstadoSincronizacion.SUCCESS, "/fixtures?league="
                    + properties.worldCup().leagueId() + "&season=" + properties.worldCup().season(), processed, "Fixtures sincronizados.", started);
        } catch (RuntimeException exception) {
            return saveLog(TipoSincronizacion.FIXTURES, EstadoSincronizacion.FAILED, "/fixtures", 0, exception.getMessage(), started);
        }
    }

    @CacheEvict(value = {"upcomingMatches", "ranking"}, allEntries = true)
    @Transactional
    public SyncResponse syncResults() {
        OffsetDateTime started = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            List<Partido> candidates = partidoRepository.findByStatusInOrKickoffAtUtcBetween(
                    List.of(EstadoPartido.LIVE, EstadoPartido.SCHEDULED),
                    started.minusHours(8),
                    started.plusHours(1));
            int processed = 0;
            for (Partido candidate : candidates) {
                JsonNode payload = apiFootballClient.fixtureById(candidate.getId());
                processed += processResponse(payload, true);
            }
            return saveLog(TipoSincronizacion.RESULTS, EstadoSincronizacion.SUCCESS, "/fixtures?id={fixtureId}", processed, "Resultados actualizados.", started);
        } catch (RuntimeException exception) {
            return saveLog(TipoSincronizacion.RESULTS, EstadoSincronizacion.FAILED, "/fixtures?id={fixtureId}", 0, exception.getMessage(), started);
        }
    }

    private int processResponse(JsonNode payload, boolean evaluateResults) {
        JsonNode response = payload == null ? null : payload.path("response");
        if (response == null || !response.isArray()) {
            return 0;
        }
        int processed = 0;
        for (JsonNode fixtureNode : response) {
            Partido match = upsertFixture(fixtureNode);
            if (match != null) {
                processed++;
                if (evaluateResults && match.getStatus() == EstadoPartido.FINISHED) {
                    globalPredictionService.evaluateMatch(match);
                }
            }
        }
        return processed;
    }

    private Partido upsertFixture(JsonNode node) {
        Long fixtureId = longValue(node.path("fixture").path("id"));
        if (fixtureId == null) {
            return null;
        }

        Competicion competition = upsertCompetition(node.path("league"));
        Equipo home = upsertTeam(node.path("teams").path("home"));
        Equipo away = upsertTeam(node.path("teams").path("away"));
        if (home == null || away == null) {
            return null;
        }

        Partido match = partidoRepository.findById(fixtureId).orElseGet(Partido::new);
        match.setId(fixtureId);
        match.setCompetition(competition);
        match.setStage(textValue(node.path("league").path("round")));
        match.setRoundName(textValue(node.path("league").path("round")));
        match.setGroupName(extractGroup(textValue(node.path("league").path("round"))));
        match.setKickoffAtUtc(OffsetDateTime.parse(textValue(node.path("fixture").path("date"))).withOffsetSameInstant(ZoneOffset.UTC));
        match.setTimezone(textValue(node.path("fixture").path("timezone")));
        match.setVenueName(textValue(node.path("fixture").path("venue").path("name")));
        match.setVenueCity(textValue(node.path("fixture").path("venue").path("city")));
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setApiStatusShort(textValue(node.path("fixture").path("status").path("short")));
        match.setApiStatusLong(textValue(node.path("fixture").path("status").path("long")));
        match.setStatus(mapStatus(match.getApiStatusShort()));
        match.setLastSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        Partido saved = partidoRepository.save(match);

        if (saved.getStatus() == EstadoPartido.FINISHED) {
            upsertResult(saved, node);
        }
        return saved;
    }

    private Competicion upsertCompetition(JsonNode leagueNode) {
        Integer leagueId = intValue(leagueNode.path("id"));
        Competicion competition = leagueId == null
                ? new Competicion()
                : competicionRepository.findByApiFootballLeagueId(leagueId).orElseGet(Competicion::new);
        competition.setApiFootballLeagueId(leagueId);
        competition.setName(valueOrDefault(textValue(leagueNode.path("name")), "FIFA World Cup"));
        competition.setSeason(intValue(leagueNode.path("season")) == null ? properties.worldCup().season() : intValue(leagueNode.path("season")));
        competition.setCountry(textValue(leagueNode.path("country")));
        competition.setLogoUrl(textValue(leagueNode.path("logo")));
        competition.setType("Cup");
        return competicionRepository.save(competition);
    }

    private Equipo upsertTeam(JsonNode teamNode) {
        Integer apiId = intValue(teamNode.path("id"));
        String name = textValue(teamNode.path("name"));
        if (apiId == null || name == null) {
            return null;
        }
        String fifaCode = fifaCodeFor(apiId, name);
        Equipo team = equipoRepository.findById(fifaCode)
                .or(() -> equipoRepository.findByApiFootballId(apiId))
                .orElseGet(Equipo::new);
        team.setFifaCode(fifaCode);
        team.setApiFootballId(apiId);
        team.setName(name);
        team.setCountry(name);
        team.setFlagUrl(textValue(teamNode.path("logo")));
        return equipoRepository.save(team);
    }

    private void upsertResult(Partido match, JsonNode node) {
        Integer homeGoals = intValue(node.path("score").path("fulltime").path("home"));
        Integer awayGoals = intValue(node.path("score").path("fulltime").path("away"));
        if (homeGoals == null || awayGoals == null) {
            homeGoals = intValue(node.path("goals").path("home"));
            awayGoals = intValue(node.path("goals").path("away"));
        }
        if (homeGoals == null || awayGoals == null) {
            return;
        }

        ResultadoPartido result = resultadoRepository.findByMatchId(match.getId()).orElseGet(ResultadoPartido::new);
        result.setMatch(match);
        result.setHomeGoals90(homeGoals);
        result.setAwayGoals90(awayGoals);
        result.setWinner90(winnerOf(homeGoals, awayGoals));
        result.setSource("API_FOOTBALL");
        result.setConfirmedAt(OffsetDateTime.now(ZoneOffset.UTC));
        resultadoRepository.save(result);
    }

    private SyncResponse saveLog(TipoSincronizacion type, EstadoSincronizacion status, String path, int processed, String message, OffsetDateTime started) {
        ApiSyncLog log = new ApiSyncLog();
        log.setSyncType(type);
        log.setStatus(status);
        log.setRequestPath(path);
        log.setRecordsProcessed(processed);
        log.setMessage(message);
        log.setStartedAt(started);
        log.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        ApiSyncLog saved = syncLogRepository.save(log);
        auditService.record(
                null,
                type == TipoSincronizacion.RESULTS ? "SYNC_RESULTS" : "SYNC_FIXTURES",
                "API_SYNC_LOG",
                saved.getId().toString(),
                "Sincronizacion " + type + " en " + path,
                status + ". Registros procesados: " + processed);
        return new SyncResponse(saved.getSyncType(), saved.getStatus(), saved.getRecordsProcessed(), saved.getMessage(), saved.getStartedAt(), saved.getFinishedAt());
    }

    private EstadoPartido mapStatus(String status) {
        if (status == null) {
            return EstadoPartido.SCHEDULED;
        }
        return switch (status) {
            case "1H", "2H", "HT", "ET", "P", "BT", "LIVE" -> EstadoPartido.LIVE;
            case "FT", "AET", "PEN" -> EstadoPartido.FINISHED;
            case "PST", "TBD" -> EstadoPartido.POSTPONED;
            case "CANC" -> EstadoPartido.CANCELLED;
            case "ABD", "SUSP", "INT" -> EstadoPartido.SUSPENDED;
            default -> EstadoPartido.SCHEDULED;
        };
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

    private String fifaCodeFor(Integer apiId, String name) {
        Integer colombiaTeamId = properties.colombia() == null ? null : properties.colombia().teamId();
        if ("Colombia".equalsIgnoreCase(name) || apiId.equals(colombiaTeamId)) {
            return "COL";
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        String prefix = normalized.length() >= 3 ? normalized.substring(0, 3) : normalized;
        return (prefix + apiId).substring(0, Math.min(10, (prefix + apiId).length()));
    }

    private String extractGroup(String round) {
        if (round == null) {
            return null;
        }
        return round.toLowerCase(Locale.ROOT).contains("group") ? round : null;
    }

    private Integer intValue(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private Long longValue(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asLong();
    }

    private String textValue(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
