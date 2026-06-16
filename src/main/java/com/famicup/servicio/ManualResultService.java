package com.famicup.servicio;

import com.famicup.modelo.dto.GuardarResultadoManualRequest;
import com.famicup.modelo.dto.ResultadoManualResponse;
import com.famicup.modelo.dto.ScoringRecalculationResponse;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.ResultadoPartido;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPartido;
import com.famicup.modelo.enumeracion.GanadorPartido;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.repositorio.PartidoRepository;
import com.famicup.repositorio.ResultadoPartidoRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualResultService {

    private final PartidoService partidoService;
    private final PartidoRepository partidoRepository;
    private final ResultadoPartidoRepository resultRepository;
    private final PredictionScoringService scoringService;
    private final PartidoMapper partidoMapper;
    private final AuditService auditService;

    public ManualResultService(
            PartidoService partidoService,
            PartidoRepository partidoRepository,
            ResultadoPartidoRepository resultRepository,
            PredictionScoringService scoringService,
            PartidoMapper partidoMapper,
            AuditService auditService) {
        this.partidoService = partidoService;
        this.partidoRepository = partidoRepository;
        this.resultRepository = resultRepository;
        this.scoringService = scoringService;
        this.partidoMapper = partidoMapper;
        this.auditService = auditService;
    }

    @CacheEvict(value = {"upcomingMatches", "ranking"}, allEntries = true)
    @Transactional
    public ResultadoManualResponse saveManualResult(Usuario admin, Long matchId, GuardarResultadoManualRequest request) {
        Partido match = partidoService.getRequired(matchId);
        ResultadoPartido result = resultRepository.findByMatchId(matchId).orElseGet(ResultadoPartido::new);
        String previous = result.getId() == null
                ? "sin resultado previo"
                : result.getHomeGoals90() + "-" + result.getAwayGoals90() + " (" + result.getSource() + ")";

        result.setMatch(match);
        result.setHomeGoals90(request.homeGoals90());
        result.setAwayGoals90(request.awayGoals90());
        result.setWinner90(winnerOf(request.homeGoals90(), request.awayGoals90()));
        result.setSource("MANUAL_ADMIN");
        result.setConfirmedAt(OffsetDateTime.now(ZoneOffset.UTC));
        ResultadoPartido saved = resultRepository.save(result);

        match.setStatus(EstadoPartido.FINISHED);
        match.setApiStatusShort(match.getApiStatusShort() == null ? "FT" : match.getApiStatusShort());
        match.setApiStatusLong("Final manual ADMIN");
        partidoRepository.save(match);

        auditService.record(
                admin,
                "MANUAL_MATCH_RESULT_SAVE",
                "MATCH_RESULT",
                matchId.toString(),
                "Resultado manual " + request.homeGoals90() + "-" + request.awayGoals90() + ". Anterior: " + previous,
                "Resultado guardado; recalculo de puntos iniciado");

        ScoringRecalculationResponse recalculation = scoringService.recalculateMatch(match, admin, false, "SCORING_RECALCULATE_MANUAL_RESULT");
        return new ResultadoManualResponse(partidoMapper.toDto(match), partidoMapper.toResultDto(saved), recalculation);
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
}
