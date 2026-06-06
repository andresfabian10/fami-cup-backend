package com.famicup.servicio;

import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.dto.GuardarPronosticoGlobalRequest;
import com.famicup.modelo.dto.PronosticoGlobalResponse;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.ResultadoPartido;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPronostico;
import com.famicup.modelo.enumeracion.GanadorPartido;
import com.famicup.modelo.mapper.ApuestaMapper;
import com.famicup.repositorio.PronosticoGlobalRepository;
import com.famicup.repositorio.ResultadoPartidoRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalPredictionService {

    private final PronosticoGlobalRepository predictionRepository;
    private final ResultadoPartidoRepository resultRepository;
    private final PartidoService partidoService;
    private final BettingParametersService parametersService;
    private final RankingService rankingService;
    private final ApuestaMapper apuestaMapper;
    private final AuditService auditService;

    public GlobalPredictionService(
            PronosticoGlobalRepository predictionRepository,
            ResultadoPartidoRepository resultRepository,
            PartidoService partidoService,
            BettingParametersService parametersService,
            RankingService rankingService,
            ApuestaMapper apuestaMapper,
            AuditService auditService) {
        this.predictionRepository = predictionRepository;
        this.resultRepository = resultRepository;
        this.partidoService = partidoService;
        this.parametersService = parametersService;
        this.rankingService = rankingService;
        this.apuestaMapper = apuestaMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PronosticoGlobalResponse> history(Usuario user) {
        return predictionRepository.findByUserOrderByRegisteredAtDesc(user).stream()
                .map(apuestaMapper::toGlobalResponse)
                .toList();
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public PronosticoGlobalResponse savePrediction(Usuario user, GuardarPronosticoGlobalRequest request) {
        Partido match = partidoService.getRequired(request.matchId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (partidoService.isClosedForBetting(match, now)) {
            throw new ReglaNegocioException("Los pronosticos de este partido ya estan cerrados.");
        }
        if (partidoService.isColombiaMatch(match)) {
            throw new ReglaNegocioException("Para partidos de Colombia usa Colombia Especial y marca una apuesta principal.");
        }

        PronosticoGlobal prediction = predictionRepository.findByUserAndMatch(user, match)
                .orElseGet(PronosticoGlobal::new);
        prediction.setUser(user);
        prediction.setMatch(match);
        prediction.setPredictedHomeGoals(request.homeGoals());
        prediction.setPredictedAwayGoals(request.awayGoals());
        prediction.setStatus(EstadoPronostico.VALID);
        prediction.setRegisteredAt(prediction.getRegisteredAt() == null ? now : prediction.getRegisteredAt());
        prediction.setPoints(0);
        prediction.setExactHit(false);
        prediction.setWinnerHit(false);
        prediction.setEvaluatedAt(null);
        PronosticoGlobal saved = predictionRepository.save(prediction);
        auditService.record(
                user,
                "GLOBAL_PREDICTION_SAVE",
                "GLOBAL_PREDICTION",
                saved.getId().toString(),
                "Guardo pronostico " + request.homeGoals() + "-" + request.awayGoals() + " para partido " + match.getId(),
                "Pronostico global guardado");
        return apuestaMapper.toGlobalResponse(saved);
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public void evaluateMatch(Partido match) {
        ResultadoPartido result = resultRepository.findByMatchId(match.getId()).orElse(null);
        if (result == null) {
            return;
        }
        List<PronosticoGlobal> predictions = predictionRepository.findByMatch(match);
        for (PronosticoGlobal prediction : predictions) {
            evaluatePrediction(prediction, result);
        }
        rankingService.recalculateAll();
    }

    private void evaluatePrediction(PronosticoGlobal prediction, ResultadoPartido result) {
        boolean exact = prediction.getPredictedHomeGoals().equals(result.getHomeGoals90())
                && prediction.getPredictedAwayGoals().equals(result.getAwayGoals90());
        boolean winner = winnerOf(prediction.getPredictedHomeGoals(), prediction.getPredictedAwayGoals()) == result.getWinner90();
        int points = exact ? parametersService.getParameters().exactPoints() : (winner ? parametersService.getParameters().winnerPoints() : 0);

        prediction.setExactHit(exact);
        prediction.setWinnerHit(!exact && winner);
        prediction.setPoints(points);
        prediction.setStatus(EstadoPronostico.EVALUATED);
        prediction.setEvaluatedAt(OffsetDateTime.now(ZoneOffset.UTC));
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
