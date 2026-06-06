package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.dto.ActualizarApuestaColombiaRequest;
import com.famicup.modelo.dto.CrearApuestasColombiaRequest;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Equipo;
import com.famicup.modelo.entidad.Pago;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoApuestaColombia;
import com.famicup.modelo.mapper.ApuestaMapper;
import com.famicup.repositorio.ApuestaColombiaRepository;
import com.famicup.repositorio.PagoRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ColombiaBetsServiceTest {

    @Mock
    private ApuestaColombiaRepository betRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PronosticoGlobalRepository predictionRepository;
    @Mock
    private PartidoService partidoService;
    @Mock
    private BettingParametersService parametersService;
    @Mock
    private ApuestaMapper apuestaMapper;
    @Mock
    private AuditService auditService;

    private ColombiaBetsService service;

    @BeforeEach
    void setUp() {
        service = new ColombiaBetsService(betRepository, pagoRepository, predictionRepository, partidoService, parametersService, apuestaMapper, auditService);
        lenient().when(predictionRepository.findByUserAndMatch(any(), any())).thenReturn(Optional.empty());
        lenient().when(betRepository.findFirstByUserAndMatchAndPrincipalGlobalPredictionTrue(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void rejectsNonColombiaMatch() {
        Partido match = match("BRA", "ARG");
        when(partidoService.getRequired(10L)).thenReturn(match);
        when(partidoService.isColombiaMatch(match)).thenReturn(false);

        assertThatThrownBy(() -> service.createBets(new Usuario(), request(10L, 1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Colombia");
    }

    @Test
    void rejectsMoreThanConfiguredMaxBets() {
        Usuario user = new Usuario();
        Partido match = match("COL", "ARG");
        when(partidoService.getRequired(10L)).thenReturn(match);
        when(partidoService.isColombiaMatch(match)).thenReturn(true);
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(false);
        when(parametersService.colombiaMaxBetsPerMatch()).thenReturn(3);
        when(betRepository.findByUserAndMatch(user, match)).thenReturn(List.of(
                bet(user, match, 0, 0),
                bet(user, match, 1, 1)));

        assertThatThrownBy(() -> service.createBets(user, request(10L, 2)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("maximo 3");
    }

    @Test
    void rejectsDuplicatedScoresInCreateRequest() {
        Usuario user = user();
        Partido match = match("COL", "ARG");
        when(partidoService.getRequired(10L)).thenReturn(match);
        when(partidoService.isColombiaMatch(match)).thenReturn(true);
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(false);
        when(parametersService.colombiaMaxBetsPerMatch()).thenReturn(3);
        when(betRepository.findByUserAndMatch(user, match)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createBets(user, new CrearApuestasColombiaRequest(
                10L,
                List.of(
                        new CrearApuestasColombiaRequest.MarcadorRequest(2, 1, null),
                        new CrearApuestasColombiaRequest.MarcadorRequest(2, 1, null)))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("repetir");
    }

    @Test
    void rejectsDuplicatedScoreAgainstExistingBet() {
        Usuario user = user();
        Partido match = match("COL", "ARG");
        when(partidoService.getRequired(10L)).thenReturn(match);
        when(partidoService.isColombiaMatch(match)).thenReturn(true);
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(false);
        when(parametersService.colombiaMaxBetsPerMatch()).thenReturn(3);
        when(betRepository.findByUserAndMatch(user, match)).thenReturn(List.of(bet(user, match, 2, 1)));

        assertThatThrownBy(() -> service.createBets(user, new CrearApuestasColombiaRequest(
                10L,
                List.of(new CrearApuestasColombiaRequest.MarcadorRequest(2, 1, null)))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("repetir");
    }

    @Test
    void createsPendingPaymentForValidBet() {
        Usuario user = new Usuario();
        Partido match = match("COL", "ARG");
        when(partidoService.getRequired(10L)).thenReturn(match);
        when(partidoService.isColombiaMatch(match)).thenReturn(true);
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(false);
        when(parametersService.colombiaMaxBetsPerMatch()).thenReturn(3);
        when(parametersService.colombiaBetAmount()).thenReturn(BigDecimal.valueOf(5000));
        when(betRepository.findByUserAndMatch(user, match)).thenReturn(List.of());
        when(betRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createBets(user, request(10L, 1));

        verify(betRepository).save(any());
        verify(pagoRepository).save(any());
    }

    @Test
    void updatesOwnBetBeforeClosing() {
        Usuario user = user();
        ApuestaColombia bet = bet(user, match("COL", "ARG"));
        UUID betId = UUID.randomUUID();
        bet.setId(betId);
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(false);
        when(partidoService.isColombiaMatch(bet.getMatch())).thenReturn(true);
        when(betRepository.findByUserAndMatch(user, bet.getMatch())).thenReturn(List.of(bet));

        service.updateBet(user, betId, new ActualizarApuestaColombiaRequest(3, 1, null));

        org.assertj.core.api.Assertions.assertThat(bet.getPredictedHomeGoals()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(bet.getPredictedAwayGoals()).isEqualTo(1);
        verify(apuestaMapper).toColombiaResponse(bet);
    }

    @Test
    void rejectsEditionWithDuplicatedScore() {
        Usuario user = user();
        Partido match = match("COL", "ARG");
        ApuestaColombia bet = bet(user, match, 1, 0);
        ApuestaColombia existing = bet(user, match, 2, 1);
        UUID betId = UUID.randomUUID();
        bet.setId(betId);
        existing.setId(UUID.randomUUID());
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(false);
        when(partidoService.isColombiaMatch(match)).thenReturn(true);
        when(betRepository.findByUserAndMatch(user, match)).thenReturn(List.of(bet, existing));

        assertThatThrownBy(() -> service.updateBet(user, betId, new ActualizarApuestaColombiaRequest(2, 1, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("repetir");
        verify(apuestaMapper, never()).toColombiaResponse(any());
    }

    @Test
    void rejectsBetEditionAfterClosing() {
        Usuario user = user();
        ApuestaColombia bet = bet(user, match("COL", "ARG"));
        UUID betId = UUID.randomUUID();
        bet.setId(betId);
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.updateBet(user, betId, new ActualizarApuestaColombiaRequest(3, 1, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cerrada");
        verify(apuestaMapper, never()).toColombiaResponse(any());
    }

    @Test
    void deletesOwnBetBeforeClosingWhenAnotherBetRemains() {
        Usuario user = user();
        Partido match = match("COL", "ARG");
        ApuestaColombia bet = bet(user, match, 1, 0);
        UUID betId = UUID.randomUUID();
        bet.setId(betId);
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(partidoService.isColombiaMatch(match)).thenReturn(true);
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(false);
        when(betRepository.findByUserAndMatch(user, match)).thenReturn(List.of(bet));

        service.deleteBet(user, betId);

        verify(betRepository).delete(bet);
    }

    @Test
    void allowsDeletingLastBetForMatch() {
        Usuario user = user();
        Partido match = match("COL", "ARG");
        ApuestaColombia bet = bet(user, match, 1, 0);
        Pago payment = new Pago();
        UUID betId = UUID.randomUUID();
        bet.setId(betId);
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(partidoService.isColombiaMatch(match)).thenReturn(true);
        when(partidoService.isClosedForBetting(any(), any())).thenReturn(false);
        when(pagoRepository.findByColombiaBet(bet)).thenReturn(Optional.of(payment));
        when(betRepository.findByUserAndMatch(user, match)).thenReturn(List.of(bet));

        service.deleteBet(user, betId);

        verify(pagoRepository).delete(payment);
        verify(betRepository).delete(bet);
    }

    private CrearApuestasColombiaRequest request(Long matchId, int count) {
        List<CrearApuestasColombiaRequest.MarcadorRequest> bets = java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> new CrearApuestasColombiaRequest.MarcadorRequest(2, index, index == 0))
                .toList();
        return new CrearApuestasColombiaRequest(matchId, bets);
    }

    private Partido match(String homeCode, String awayCode) {
        Equipo home = new Equipo();
        home.setFifaCode(homeCode);
        home.setName(homeCode);
        Equipo away = new Equipo();
        away.setFifaCode(awayCode);
        away.setName(awayCode);
        Partido match = new Partido();
        match.setId(10L);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setKickoffAtUtc(OffsetDateTime.now(ZoneOffset.UTC).plusHours(2));
        return match;
    }

    private Usuario user() {
        Usuario user = new Usuario();
        user.setId(UUID.randomUUID());
        user.setUsername("player");
        return user;
    }

    private ApuestaColombia bet(Usuario user, Partido match) {
        return bet(user, match, 1, 0);
    }

    private ApuestaColombia bet(Usuario user, Partido match, int homeGoals, int awayGoals) {
        ApuestaColombia bet = new ApuestaColombia();
        bet.setUser(user);
        bet.setMatch(match);
        bet.setPredictedHomeGoals(homeGoals);
        bet.setPredictedAwayGoals(awayGoals);
        bet.setStatus(EstadoApuestaColombia.PENDING_PAYMENT);
        return bet;
    }
}
