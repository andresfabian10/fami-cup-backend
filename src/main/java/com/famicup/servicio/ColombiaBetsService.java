package com.famicup.servicio;

import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.modelo.dto.ActualizarApuestaColombiaRequest;
import com.famicup.modelo.dto.ApuestaColombiaResponse;
import com.famicup.modelo.dto.CrearApuestasColombiaRequest;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Pago;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoApuestaColombia;
import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.enumeracion.EstadoPronostico;
import com.famicup.modelo.enumeracion.OrigenRegistro;
import com.famicup.modelo.enumeracion.SistemaPago;
import com.famicup.modelo.mapper.ApuestaMapper;
import com.famicup.repositorio.ApuestaColombiaRepository;
import com.famicup.repositorio.PagoRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColombiaBetsService {

    private final ApuestaColombiaRepository betRepository;
    private final PagoRepository pagoRepository;
    private final PronosticoGlobalRepository predictionRepository;
    private final PartidoService partidoService;
    private final BettingParametersService parametersService;
    private final ApuestaMapper apuestaMapper;
    private final AuditService auditService;
    private final PredictionScoringService scoringService;
    private final RankingService rankingService;

    public ColombiaBetsService(
            ApuestaColombiaRepository betRepository,
            PagoRepository pagoRepository,
            PronosticoGlobalRepository predictionRepository,
            PartidoService partidoService,
            BettingParametersService parametersService,
            ApuestaMapper apuestaMapper,
            AuditService auditService,
            PredictionScoringService scoringService,
            RankingService rankingService) {
        this.betRepository = betRepository;
        this.pagoRepository = pagoRepository;
        this.predictionRepository = predictionRepository;
        this.partidoService = partidoService;
        this.parametersService = parametersService;
        this.apuestaMapper = apuestaMapper;
        this.auditService = auditService;
        this.scoringService = scoringService;
        this.rankingService = rankingService;
    }

    @Transactional
    public List<ApuestaColombiaResponse> createBets(Usuario user, CrearApuestasColombiaRequest request) {
        Partido match = partidoService.getRequired(request.matchId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (!partidoService.isColombiaMatch(match)) {
            throw new ReglaNegocioException("Colombia Especial solo acepta partidos donde juega Colombia.");
        }
        if (partidoService.isClosedForBetting(match, now)) {
            throw new ReglaNegocioException("Las apuestas de este partido ya estan cerradas.");
        }

        int maxBets = parametersService.colombiaMaxBetsPerMatch();
        List<ApuestaColombia> existingBets = betRepository.findByUserAndMatch(user, match);
        validateUniqueScores(request.bets(), existingBets, null);
        validatePrincipalRequest(request.bets(), existingBets);
        long existing = existingBets.size();
        if (request.bets().isEmpty() || existing + request.bets().size() > maxBets) {
            throw new ReglaNegocioException("Solo puedes registrar maximo " + maxBets + " apuestas por partido de Colombia.");
        }

        if (hasRequestedPrincipal(request.bets())) {
            clearPrincipalForUserAndMatch(user, match, existingBets);
        }

        List<ApuestaColombia> savedBets = new ArrayList<>();
        for (CrearApuestasColombiaRequest.MarcadorRequest score : request.bets()) {
            savedBets.add(createSingleBet(user, match, score, now, OrigenRegistro.PLAYER, null));
        }
        normalizePrincipal(user, match);
        syncPrincipalGlobalPrediction(user, match, now);
        auditService.record(
                user,
                "COLOMBIA_BET_CREATE",
                "COLOMBIA_BET",
                String.valueOf(match.getId()),
                "Registro " + savedBets.size() + " apuesta(s) Colombia",
                "Apuestas Colombia guardadas");

        return savedBets.stream().map(apuestaMapper::toColombiaResponse).toList();
    }

    @Transactional
    public List<ApuestaColombiaResponse> createBetsForAdmin(Usuario admin, Usuario player, CrearApuestasColombiaRequest request) {
        Partido match = partidoService.getRequired(request.matchId());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (!partidoService.isColombiaMatch(match)) {
            throw new ReglaNegocioException("Colombia Especial solo acepta partidos donde juega Colombia.");
        }

        int maxBets = parametersService.colombiaMaxBetsPerMatch();
        List<ApuestaColombia> existingBets = betRepository.findByUserAndMatch(player, match);
        validateUniqueScores(request.bets(), existingBets, null);
        validatePrincipalRequest(request.bets(), existingBets);
        long existing = existingBets.size();
        if (request.bets().isEmpty() || existing + request.bets().size() > maxBets) {
            throw new ReglaNegocioException("Solo se pueden registrar maximo " + maxBets + " apuestas por partido de Colombia.");
        }

        if (hasRequestedPrincipal(request.bets())) {
            clearPrincipalForUserAndMatch(player, match, existingBets);
        }

        List<ApuestaColombia> savedBets = new ArrayList<>();
        for (CrearApuestasColombiaRequest.MarcadorRequest score : request.bets()) {
            savedBets.add(createSingleBet(player, match, score, now, OrigenRegistro.ADMIN, admin));
        }
        normalizePrincipal(player, match);
        syncPrincipalGlobalPrediction(player, match, now);
        auditService.record(
                admin,
                "MANUAL_COLOMBIA_BET_CREATE",
                "COLOMBIA_BET",
                String.valueOf(match.getId()),
                "Registro manual " + savedBets.size() + " apuesta(s) Colombia para " + player.getUsername(),
                "Apuestas Colombia guardadas por ADMIN");

        return savedBets.stream().map(apuestaMapper::toColombiaResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ApuestaColombiaResponse> history(Usuario user) {
        return betRepository.findByUserOrderByRegisteredAtDesc(user).stream()
                .map(apuestaMapper::toColombiaResponse)
                .toList();
    }

    @Transactional
    public ApuestaColombiaResponse updateBet(Usuario user, UUID betId, ActualizarApuestaColombiaRequest request) {
        ApuestaColombia bet = betRepository.findById(betId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Apuesta Colombia no encontrada."));
        if (!bet.getUser().getId().equals(user.getId())) {
            throw new ReglaNegocioException("Solo puedes editar tus propias apuestas.");
        }
        if (partidoService.isClosedForBetting(bet.getMatch(), OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ReglaNegocioException("Esta apuesta ya esta cerrada y no se puede editar.");
        }
        if (!partidoService.isColombiaMatch(bet.getMatch())) {
            throw new ReglaNegocioException("Colombia Especial solo acepta partidos donde juega Colombia.");
        }
        if (bet.getStatus() == EstadoApuestaColombia.ANNULLED
                || bet.getStatus() == EstadoApuestaColombia.WON
                || bet.getStatus() == EstadoApuestaColombia.LOST) {
            throw new ReglaNegocioException("Esta apuesta ya no esta disponible para edicion.");
        }
        List<ApuestaColombia> allBets = betRepository.findByUserAndMatch(user, bet.getMatch());
        validateUniqueScore(request.homeGoals(), request.awayGoals(), allBets, bet.getId());

        bet.setPredictedHomeGoals(request.homeGoals());
        bet.setPredictedAwayGoals(request.awayGoals());
        if (Boolean.TRUE.equals(request.principalGlobalPrediction())) {
            clearPrincipalForUserAndMatch(user, bet.getMatch(), allBets);
            bet.setPrincipalGlobalPrediction(true);
        }
        normalizePrincipal(user, bet.getMatch());
        syncPrincipalGlobalPrediction(user, bet.getMatch(), OffsetDateTime.now(ZoneOffset.UTC));
        auditService.record(
                user,
                "COLOMBIA_BET_UPDATE",
                "COLOMBIA_BET",
                bet.getId().toString(),
                "Actualizo marcador a " + request.homeGoals() + "-" + request.awayGoals(),
                "Apuesta Colombia actualizada");
        return apuestaMapper.toColombiaResponse(bet);
    }

    @Transactional
    public ApuestaColombiaResponse updateBetForAdmin(Usuario admin, UUID betId, ActualizarApuestaColombiaRequest request) {
        ApuestaColombia bet = betRepository.findById(betId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Apuesta Colombia no encontrada."));
        if (!partidoService.isColombiaMatch(bet.getMatch())) {
            throw new ReglaNegocioException("Colombia Especial solo acepta partidos donde juega Colombia.");
        }
        if (bet.getStatus() == EstadoApuestaColombia.WON || bet.getStatus() == EstadoApuestaColombia.LOST) {
            throw new ReglaNegocioException("Esta apuesta ya no esta disponible para correccion manual.");
        }

        Usuario player = bet.getUser();
        List<ApuestaColombia> allBets = betRepository.findByUserAndMatch(player, bet.getMatch());
        validateUniqueScore(request.homeGoals(), request.awayGoals(), allBets, bet.getId());

        bet.setPredictedHomeGoals(request.homeGoals());
        bet.setPredictedAwayGoals(request.awayGoals());
        bet.setUpdatedByAdmin(admin);
        if (Boolean.TRUE.equals(request.principalGlobalPrediction())) {
            clearPrincipalForUserAndMatch(player, bet.getMatch(), allBets);
            bet.setPrincipalGlobalPrediction(true);
        }
        normalizePrincipal(player, bet.getMatch());
        syncPrincipalGlobalPrediction(player, bet.getMatch(), OffsetDateTime.now(ZoneOffset.UTC));
        auditService.record(
                admin,
                "MANUAL_COLOMBIA_BET_UPDATE",
                "COLOMBIA_BET",
                bet.getId().toString(),
                "Corrigio manualmente apuesta de " + player.getUsername() + " a " + request.homeGoals() + "-" + request.awayGoals(),
                "Apuesta Colombia actualizada por ADMIN");
        return apuestaMapper.toColombiaResponse(bet);
    }

    @Transactional
    public void deleteBet(Usuario user, UUID betId) {
        ApuestaColombia bet = betRepository.findById(betId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Apuesta Colombia no encontrada."));
        if (!bet.getUser().getId().equals(user.getId())) {
            throw new ReglaNegocioException("Solo puedes eliminar tus propias apuestas.");
        }
        if (!partidoService.isColombiaMatch(bet.getMatch())) {
            throw new ReglaNegocioException("Colombia Especial solo acepta partidos donde juega Colombia.");
        }
        if (partidoService.isClosedForBetting(bet.getMatch(), OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new ReglaNegocioException("Esta apuesta ya esta cerrada y no se puede eliminar.");
        }
        if (bet.getStatus() == EstadoApuestaColombia.WON || bet.getStatus() == EstadoApuestaColombia.LOST) {
            throw new ReglaNegocioException("Esta apuesta ya no esta disponible para eliminacion.");
        }

        Partido match = bet.getMatch();
        boolean wasPrincipal = bet.isPrincipalGlobalPrediction();
        List<ApuestaColombia> remainingBets = betRepository.findByUserAndMatch(user, match).stream()
                .filter(currentBet -> !currentBet.getId().equals(bet.getId()))
                .toList();

        if (remainingBets.isEmpty()) {
            pagoRepository.findByColombiaBet(bet).ifPresent(pagoRepository::delete);
            betRepository.delete(bet);
            predictionRepository.findByUserAndMatch(user, match).ifPresent(predictionRepository::delete);
        } else {
            if (wasPrincipal || remainingBets.stream().noneMatch(ApuestaColombia::isPrincipalGlobalPrediction)) {
                clearPrincipalForUserAndMatch(user, match, remainingBets);
                bet.setPrincipalGlobalPrediction(false);
                remainingBets.get(0).setPrincipalGlobalPrediction(true);
            }
            pagoRepository.findByColombiaBet(bet).ifPresent(pagoRepository::delete);
            betRepository.delete(bet);
            syncPrincipalGlobalPrediction(user, match, OffsetDateTime.now(ZoneOffset.UTC));
        }
        auditService.record(
                user,
                "COLOMBIA_BET_DELETE",
                "COLOMBIA_BET",
                betId.toString(),
                "Elimino apuesta Colombia " + bet.getPredictedHomeGoals() + "-" + bet.getPredictedAwayGoals(),
                "Apuesta Colombia eliminada");
    }

    @Transactional
    public void deleteBetForAdmin(Usuario admin, UUID betId) {
        ApuestaColombia bet = betRepository.findById(betId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Apuesta Colombia no encontrada."));
        Usuario player = bet.getUser();
        Partido match = bet.getMatch();
        if (bet.getStatus() == EstadoApuestaColombia.WON || bet.getStatus() == EstadoApuestaColombia.LOST) {
            throw new ReglaNegocioException("Esta apuesta ya no esta disponible para eliminacion manual.");
        }

        boolean wasPrincipal = bet.isPrincipalGlobalPrediction();
        List<ApuestaColombia> remainingBets = betRepository.findByUserAndMatch(player, match).stream()
                .filter(currentBet -> !currentBet.getId().equals(bet.getId()))
                .toList();

        pagoRepository.findByColombiaBet(bet).ifPresent(pagoRepository::delete);
        betRepository.delete(bet);
        if (remainingBets.isEmpty()) {
            predictionRepository.findByUserAndMatch(player, match).ifPresent(predictionRepository::delete);
        } else {
            if (wasPrincipal || remainingBets.stream().noneMatch(ApuestaColombia::isPrincipalGlobalPrediction)) {
                clearPrincipalForUserAndMatch(player, match, remainingBets);
                remainingBets.get(0).setPrincipalGlobalPrediction(true);
            }
            syncPrincipalGlobalPrediction(player, match, OffsetDateTime.now(ZoneOffset.UTC));
        }
        auditService.record(
                admin,
                "MANUAL_COLOMBIA_BET_DELETE",
                "COLOMBIA_BET",
                betId.toString(),
                "Elimino manualmente apuesta Colombia de " + player.getUsername(),
                "Apuesta Colombia eliminada por ADMIN");
    }

    private ApuestaColombia createSingleBet(
            Usuario user,
            Partido match,
            CrearApuestasColombiaRequest.MarcadorRequest score,
            OffsetDateTime now,
            OrigenRegistro origin,
            Usuario admin) {
        ApuestaColombia bet = new ApuestaColombia();
        bet.setUser(user);
        bet.setMatch(match);
        bet.setPredictedHomeGoals(score.homeGoals());
        bet.setPredictedAwayGoals(score.awayGoals());
        bet.setAmountCop(parametersService.colombiaBetAmount());
        bet.setStatus(EstadoApuestaColombia.PENDING_PAYMENT);
        bet.setPaymentStatus(EstadoPago.PENDING);
        bet.setValid(false);
        bet.setPrincipalGlobalPrediction(Boolean.TRUE.equals(score.principalGlobalPrediction()));
        bet.setEntryOrigin(origin);
        bet.setCreatedByAdmin(admin);
        bet.setUpdatedByAdmin(admin);
        bet.setRegisteredAt(now);
        ApuestaColombia saved = betRepository.save(bet);

        Pago payment = new Pago();
        payment.setUser(user);
        payment.setSystem(SistemaPago.COLOMBIA);
        payment.setMatch(match);
        payment.setColombiaBet(saved);
        payment.setAmountCop(saved.getAmountCop());
        payment.setStatus(EstadoPago.PENDING);
        payment.setPaymentMethod("Por confirmar");
        pagoRepository.save(payment);

        return saved;
    }

    private void validateUniqueScores(
            List<CrearApuestasColombiaRequest.MarcadorRequest> requestedScores,
            List<ApuestaColombia> existingBets,
            UUID ignoredBetId) {
        Set<String> requestedKeys = new HashSet<>();
        for (CrearApuestasColombiaRequest.MarcadorRequest score : requestedScores) {
            String key = scoreKey(score.homeGoals(), score.awayGoals());
            if (!requestedKeys.add(key)) {
                throw new ReglaNegocioException("No puedes repetir el mismo marcador en un partido de Colombia.");
            }
            validateUniqueScore(score.homeGoals(), score.awayGoals(), existingBets, ignoredBetId);
        }
    }

    private void validateUniqueScore(Integer homeGoals, Integer awayGoals, List<ApuestaColombia> existingBets, UUID ignoredBetId) {
        String key = scoreKey(homeGoals, awayGoals);
        boolean duplicated = existingBets.stream()
                .filter(existingBet -> ignoredBetId == null || !Objects.equals(existingBet.getId(), ignoredBetId))
                .anyMatch(existingBet -> scoreKey(existingBet.getPredictedHomeGoals(), existingBet.getPredictedAwayGoals()).equals(key));
        if (duplicated) {
            throw new ReglaNegocioException("No puedes repetir el mismo marcador en un partido de Colombia.");
        }
    }

    private String scoreKey(Integer homeGoals, Integer awayGoals) {
        return homeGoals + "-" + awayGoals;
    }

    private void validatePrincipalRequest(List<CrearApuestasColombiaRequest.MarcadorRequest> requestedScores, List<ApuestaColombia> existingBets) {
        long requestedPrincipalCount = requestedScores.stream()
                .filter(score -> Boolean.TRUE.equals(score.principalGlobalPrediction()))
                .count();
        if (requestedPrincipalCount > 1) {
            throw new ReglaNegocioException("Solo puedes escoger una apuesta principal por partido de Colombia.");
        }
        long existingPrincipalCount = existingBets.stream()
                .filter(ApuestaColombia::isPrincipalGlobalPrediction)
                .count();
        if (requestedPrincipalCount == 0 && existingPrincipalCount == 0 && existingBets.size() + requestedScores.size() > 1) {
            throw new ReglaNegocioException("Escoge una apuesta principal para sumar puntos en la Polla Global.");
        }
    }

    private boolean hasRequestedPrincipal(List<CrearApuestasColombiaRequest.MarcadorRequest> requestedScores) {
        return requestedScores.stream().anyMatch(score -> Boolean.TRUE.equals(score.principalGlobalPrediction()));
    }

    private void clearPrincipal(List<ApuestaColombia> bets) {
        bets.forEach(bet -> bet.setPrincipalGlobalPrediction(false));
    }

    private void clearPrincipalForUserAndMatch(Usuario user, Partido match, List<ApuestaColombia> managedBets) {
        betRepository.clearPrincipalForUserAndMatch(user, match);
        clearPrincipal(managedBets);
    }

    private void normalizePrincipal(Usuario user, Partido match) {
        List<ApuestaColombia> bets = betRepository.findByUserAndMatch(user, match);
        if (bets.isEmpty()) {
            return;
        }
        long principalCount = bets.stream().filter(ApuestaColombia::isPrincipalGlobalPrediction).count();
        if (principalCount > 1) {
            throw new ReglaNegocioException("Solo puede existir una apuesta principal por partido de Colombia.");
        }
        if (principalCount == 0) {
            if (bets.size() == 1) {
                bets.get(0).setPrincipalGlobalPrediction(true);
                return;
            }
            throw new ReglaNegocioException("Escoge una apuesta principal para sumar puntos en la Polla Global.");
        }
    }

    private void syncPrincipalGlobalPrediction(Usuario user, Partido match, OffsetDateTime now) {
        ApuestaColombia principalBet = betRepository.findFirstByUserAndMatchAndPrincipalGlobalPredictionTrue(user, match).orElse(null);
        if (principalBet == null) {
            predictionRepository.findByUserAndMatch(user, match).ifPresent(predictionRepository::delete);
            return;
        }

        PronosticoGlobal prediction = predictionRepository.findByUserAndMatch(user, match)
                .orElseGet(PronosticoGlobal::new);
        boolean newPrediction = prediction.getId() == null;
        prediction.setUser(user);
        prediction.setMatch(match);
        prediction.setPredictedHomeGoals(principalBet.getPredictedHomeGoals());
        prediction.setPredictedAwayGoals(principalBet.getPredictedAwayGoals());
        prediction.setStatus(EstadoPronostico.VALID);
        prediction.setPoints(0);
        prediction.setExactHit(false);
        prediction.setWinnerHit(false);
        prediction.setRegisteredAt(prediction.getRegisteredAt() == null ? now : prediction.getRegisteredAt());
        prediction.setEvaluatedAt(null);
        if (newPrediction) {
            prediction.setEntryOrigin(principalBet.getEntryOrigin());
            prediction.setCreatedByAdmin(principalBet.getCreatedByAdmin());
        }
        if (principalBet.getUpdatedByAdmin() != null) {
            prediction.setUpdatedByAdmin(principalBet.getUpdatedByAdmin());
        }
        boolean evaluated = scoringService.evaluateIfResultExists(prediction);
        predictionRepository.save(prediction);
        if (evaluated) {
            rankingService.recalculateAll();
        }
    }
}
