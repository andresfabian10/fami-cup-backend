package com.famicup.servicio;

import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.modelo.dto.ActualizarPronosticoGlobalManualRequest;
import com.famicup.modelo.dto.GuardarPronosticoGlobalRequest;
import com.famicup.modelo.dto.PronosticoGlobalResponse;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPronostico;
import com.famicup.modelo.enumeracion.OrigenRegistro;
import com.famicup.modelo.mapper.ApuestaMapper;
import com.famicup.repositorio.PronosticoGlobalRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlobalPredictionService {

    private final PronosticoGlobalRepository predictionRepository;
    private final PartidoService partidoService;
    private final RankingService rankingService;
    private final ApuestaMapper apuestaMapper;
    private final AuditService auditService;
    private final PredictionScoringService scoringService;

    public GlobalPredictionService(
            PronosticoGlobalRepository predictionRepository,
            PartidoService partidoService,
            RankingService rankingService,
            ApuestaMapper apuestaMapper,
            AuditService auditService,
            PredictionScoringService scoringService) {
        this.predictionRepository = predictionRepository;
        this.partidoService = partidoService;
        this.rankingService = rankingService;
        this.apuestaMapper = apuestaMapper;
        this.auditService = auditService;
        this.scoringService = scoringService;
    }

    @Transactional
    public List<PronosticoGlobalResponse> history(Usuario user) {
        scoringService.repairUserPredictionsWithResults(user);
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
        if (prediction.getId() == null) {
            prediction.setEntryOrigin(OrigenRegistro.PLAYER);
        }
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
    public PronosticoGlobalResponse savePredictionForAdmin(Usuario admin, Usuario player, GuardarPronosticoGlobalRequest request) {
        Partido match = partidoService.getRequired(request.matchId());
        if (partidoService.isColombiaMatch(match)) {
            throw new ReglaNegocioException("Para partidos de Colombia registra una apuesta Colombia y marca la principal.");
        }
        PronosticoGlobal prediction = predictionRepository.findByUserAndMatch(player, match)
                .orElseGet(PronosticoGlobal::new);
        boolean newPrediction = prediction.getId() == null;
        applyManualPredictionValues(prediction, player, match, request.homeGoals(), request.awayGoals(), admin, newPrediction);
        boolean evaluated = scoringService.evaluateIfResultExists(prediction);
        PronosticoGlobal saved = predictionRepository.save(prediction);
        if (evaluated) {
            rankingService.recalculateAll();
        }
        auditService.record(
                admin,
                "MANUAL_GLOBAL_PREDICTION_SAVE",
                "GLOBAL_PREDICTION",
                saved.getId().toString(),
                "Registro manual pronostico " + request.homeGoals() + "-" + request.awayGoals() + " para " + player.getUsername(),
                "Pronostico global guardado por ADMIN");
        return apuestaMapper.toGlobalResponse(saved);
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public PronosticoGlobalResponse updatePredictionForAdmin(Usuario admin, java.util.UUID predictionId, ActualizarPronosticoGlobalManualRequest request) {
        PronosticoGlobal prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pronostico global no encontrado."));
        Partido match = request.matchId() == null ? prediction.getMatch() : partidoService.getRequired(request.matchId());
        if (partidoService.isColombiaMatch(match)) {
            throw new ReglaNegocioException("Para partidos de Colombia registra una apuesta Colombia y marca la principal.");
        }
        predictionRepository.findByUserAndMatch(prediction.getUser(), match)
                .filter(existing -> !existing.getId().equals(prediction.getId()))
                .ifPresent(existing -> {
                    throw new ReglaNegocioException("El jugador ya tiene un pronostico global para ese partido.");
                });

        applyManualPredictionValues(prediction, prediction.getUser(), match, request.homeGoals(), request.awayGoals(), admin, false);
        boolean evaluated = scoringService.evaluateIfResultExists(prediction);
        if (evaluated) {
            rankingService.recalculateAll();
        }
        auditService.record(
                admin,
                "MANUAL_GLOBAL_PREDICTION_UPDATE",
                "GLOBAL_PREDICTION",
                prediction.getId().toString(),
                "Corrigio manualmente pronostico de " + prediction.getUser().getUsername(),
                "Pronostico global actualizado por ADMIN");
        return apuestaMapper.toGlobalResponse(prediction);
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public void deletePredictionForAdmin(Usuario admin, java.util.UUID predictionId) {
        PronosticoGlobal prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pronostico global no encontrado."));
        if (prediction.getStatus() == EstadoPronostico.EVALUATED) {
            throw new ReglaNegocioException("Este pronostico ya fue evaluado y no se puede eliminar manualmente.");
        }
        String username = prediction.getUser().getUsername();
        predictionRepository.delete(prediction);
        auditService.record(
                admin,
                "MANUAL_GLOBAL_PREDICTION_DELETE",
                "GLOBAL_PREDICTION",
                predictionId.toString(),
                "Elimino manualmente pronostico global de " + username,
                "Pronostico global eliminado por ADMIN");
    }

    @CacheEvict(value = "ranking", allEntries = true)
    @Transactional
    public void evaluateMatch(Partido match) {
        scoringService.recalculateMatch(match, null, false, "SCORING_RECALCULATE_AUTO_RESULT");
    }

    private void applyManualPredictionValues(
            PronosticoGlobal prediction,
            Usuario player,
            Partido match,
            Integer homeGoals,
            Integer awayGoals,
            Usuario admin,
            boolean newPrediction) {
        prediction.setUser(player);
        prediction.setMatch(match);
        prediction.setPredictedHomeGoals(homeGoals);
        prediction.setPredictedAwayGoals(awayGoals);
        prediction.setStatus(EstadoPronostico.VALID);
        prediction.setPoints(0);
        prediction.setExactHit(false);
        prediction.setWinnerHit(false);
        prediction.setEvaluatedAt(null);
        prediction.setUpdatedByAdmin(admin);
        if (newPrediction) {
            prediction.setEntryOrigin(OrigenRegistro.ADMIN);
            prediction.setCreatedByAdmin(admin);
            prediction.setRegisteredAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
    }

}
