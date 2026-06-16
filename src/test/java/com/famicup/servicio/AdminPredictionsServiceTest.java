package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Equipo;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoCampeonMundial;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPronostico;
import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.repositorio.ApuestaColombiaRepository;
import com.famicup.repositorio.PronosticoCampeonMundialRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import com.famicup.repositorio.ResultadoPartidoRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPredictionsServiceTest {

    @Mock
    private ApuestaColombiaRepository colombiaBetRepository;
    @Mock
    private PronosticoGlobalRepository globalPredictionRepository;
    @Mock
    private PronosticoCampeonMundialRepository championPredictionRepository;
    @Mock
    private ResultadoPartidoRepository resultRepository;
    @Mock
    private BettingParametersService parametersService;
    @Mock
    private PredictionScoringService scoringService;

    private AdminPredictionsService service;

    @BeforeEach
    void setUp() {
        PartidoMapper partidoMapper = new PartidoMapper(resultRepository);
        service = new AdminPredictionsService(
                colombiaBetRepository,
                globalPredictionRepository,
                championPredictionRepository,
                partidoMapper,
                parametersService,
                scoringService);
        lenient().when(scoringService.calculate(
                        nullable(Integer.class),
                        nullable(Integer.class),
                        nullable(Integer.class),
                        nullable(Integer.class)))
                .thenReturn(new PredictionScoringService.ScoringDecision(0, "Pendiente de resultado", "PENDING_RESULT", false, false));
    }

    @Test
    void listsUnifiedPredictionsWithSpanishTeamNames() {
        OffsetDateTime now = OffsetDateTime.parse("2029-06-01T00:00:00Z");
        Usuario player = player();
        Partido match = match(1L, team("GER", "Germany"), team("USA", "United States"));
        ApuestaColombia colombiaBet = colombiaBet(player, match, now);
        PronosticoGlobal globalPrediction = globalPrediction(player, match, now);
        PronosticoCampeonMundial championPrediction = championPrediction(player, team("JPN", "Japan"), now);

        when(parametersService.getParameters()).thenReturn(parameters());
        when(colombiaBetRepository.findAllByOrderByRegisteredAtDesc()).thenReturn(List.of(colombiaBet));
        when(globalPredictionRepository.findAllByOrderByRegisteredAtDesc()).thenReturn(List.of(globalPrediction));
        when(championPredictionRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(championPrediction));
        when(resultRepository.findByMatchId(1L)).thenReturn(Optional.empty());

        var response = service.listPredictions(null);

        assertThat(response.summary().totalPredictions()).isEqualTo(3);
        assertThat(response.summary().playersWithPredictions()).isEqualTo(1);
        assertThat(response.summary().colombiaPredictions()).isEqualTo(1);
        assertThat(response.summary().globalPredictions()).isEqualTo(1);
        assertThat(response.summary().championPredictions()).isEqualTo(1);
        assertThat(response.summary().principalColombiaPredictions()).isEqualTo(1);
        assertThat(response.summary().topMatchLabel()).isEqualTo("Alemania vs Estados Unidos");
        assertThat(response.summary().topMatchCount()).isEqualTo(2);
        assertThat(response.items())
                .anySatisfy(item -> {
                    assertThat(item.modality()).isEqualTo("COLOMBIA");
                    assertThat(item.match().homeTeam().displayName()).isEqualTo("Alemania");
                    assertThat(item.match().awayTeam().displayName()).isEqualTo("Estados Unidos");
                    assertThat(item.principalGlobalPrediction()).isTrue();
                })
                .anySatisfy(item -> {
                    assertThat(item.modality()).isEqualTo("WORLD_CHAMPION");
                    assertThat(item.predictionLabel()).isEqualTo("Japón");
                    assertThat(item.possiblePoints()).isEqualTo(10);
                });
        verify(scoringService).repairPredictionsWithResults(null, "SCORING_AUTO_REPAIR_ADMIN_PREDICTIONS_VIEW");
    }

    @Test
    void adminPredictionsUseRepairedScoringFromCentralService() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-13T20:00:00Z");
        Usuario player = player();
        Partido match = match(9L, team("SWE", "Sweden"), team("TUN", "Tunisia"));
        PronosticoGlobal prediction = globalPrediction(player, match, now);
        prediction.setPredictedHomeGoals(2);
        prediction.setPredictedAwayGoals(1);
        prediction.setPoints(0);
        prediction.setStatus(EstadoPronostico.VALID);

        when(parametersService.getParameters()).thenReturn(parameters());
        when(colombiaBetRepository.findAllByOrderByRegisteredAtDesc()).thenReturn(List.of());
        when(globalPredictionRepository.findAllByOrderByRegisteredAtDesc()).thenReturn(List.of(prediction));
        when(championPredictionRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of());
        when(resultRepository.findByMatchId(9L)).thenReturn(Optional.of(result(match, 5, 1)));
        when(scoringService.calculate(eq(5), eq(1), eq(2), eq(1)))
                .thenReturn(new PredictionScoringService.ScoringDecision(2, "Ganador acertado", "CALCULATED", false, true));
        doAnswer(invocation -> {
            prediction.setPoints(2);
            prediction.setStatus(EstadoPronostico.EVALUATED);
            prediction.setWinnerHit(true);
            return null;
        }).when(scoringService).repairPredictionsWithResults(null, "SCORING_AUTO_REPAIR_ADMIN_PREDICTIONS_VIEW");

        var response = service.listPredictions(null);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.player().fullName()).isEqualTo("Abuelo Javier");
            assertThat(item.match().homeTeam().displayName()).isEqualTo("Suecia");
            assertThat(item.match().awayTeam().displayName()).isEqualTo("Túnez");
            assertThat(item.predictionLabel()).isEqualTo("2 - 1");
            assertThat(item.resultLabel()).isEqualTo("5 - 1");
            assertThat(item.points()).isEqualTo(2);
            assertThat(item.adminStatus()).isEqualTo("WON");
            assertThat(item.pointsReason()).isEqualTo("Ganador acertado");
            assertThat(item.pointsStatus()).isEqualTo("CALCULATED");
        });
    }

    private ParametrosApuestasResponse parameters() {
        return new ParametrosApuestasResponse(
                BigDecimal.valueOf(5000),
                3,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(40000),
                10,
                5,
                2,
                50,
                30,
                20,
                5,
                10,
                OffsetDateTime.parse("2030-06-01T00:00:00Z"),
                "573163353115",
                "Recuperar",
                "Acceso",
                "Recuperar",
                "Acceso",
                false,
                "",
                "",
                "Banner",
                12);
    }

    private Usuario player() {
        Usuario user = new Usuario();
        user.setId(UUID.randomUUID());
        user.setUsername("abuelo.javier");
        user.setFullName("Abuelo Javier");
        user.setRole(RolUsuario.PLAYER);
        return user;
    }

    private Equipo team(String code, String name) {
        Equipo team = new Equipo();
        team.setFifaCode(code);
        team.setName(name);
        team.setFlagUrl("/flags/" + code.toLowerCase() + ".svg");
        return team;
    }

    private Partido match(Long id, Equipo home, Equipo away) {
        Partido match = new Partido();
        match.setId(id);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setKickoffAtUtc(OffsetDateTime.parse("2030-06-12T20:00:00Z"));
        return match;
    }

    private ApuestaColombia colombiaBet(Usuario player, Partido match, OffsetDateTime now) {
        ApuestaColombia bet = new ApuestaColombia();
        bet.setId(UUID.randomUUID());
        bet.setUser(player);
        bet.setMatch(match);
        bet.setPredictedHomeGoals(2);
        bet.setPredictedAwayGoals(1);
        bet.setAmountCop(BigDecimal.valueOf(5000));
        bet.setPrincipalGlobalPrediction(true);
        bet.setRegisteredAt(now);
        bet.setCreatedAt(now);
        bet.setUpdatedAt(now);
        return bet;
    }

    private PronosticoGlobal globalPrediction(Usuario player, Partido match, OffsetDateTime now) {
        PronosticoGlobal prediction = new PronosticoGlobal();
        prediction.setId(UUID.randomUUID());
        prediction.setUser(player);
        prediction.setMatch(match);
        prediction.setPredictedHomeGoals(1);
        prediction.setPredictedAwayGoals(1);
        prediction.setStatus(EstadoPronostico.VALID);
        prediction.setRegisteredAt(now);
        prediction.setCreatedAt(now);
        prediction.setUpdatedAt(now);
        return prediction;
    }

    private com.famicup.modelo.entidad.ResultadoPartido result(Partido match, int homeGoals, int awayGoals) {
        com.famicup.modelo.entidad.ResultadoPartido result = new com.famicup.modelo.entidad.ResultadoPartido();
        result.setMatch(match);
        result.setHomeGoals90(homeGoals);
        result.setAwayGoals90(awayGoals);
        return result;
    }

    private PronosticoCampeonMundial championPrediction(Usuario player, Equipo team, OffsetDateTime now) {
        PronosticoCampeonMundial prediction = new PronosticoCampeonMundial();
        prediction.setId(UUID.randomUUID());
        prediction.setUser(player);
        prediction.setTeam(team);
        prediction.setCreatedAt(now);
        prediction.setUpdatedAt(now);
        return prediction;
    }
}
