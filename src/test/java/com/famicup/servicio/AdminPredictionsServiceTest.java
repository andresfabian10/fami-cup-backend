package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
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

    private AdminPredictionsService service;

    @BeforeEach
    void setUp() {
        PartidoMapper partidoMapper = new PartidoMapper(resultRepository);
        service = new AdminPredictionsService(
                colombiaBetRepository,
                globalPredictionRepository,
                championPredictionRepository,
                partidoMapper,
                parametersService);
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

        var response = service.listPredictions();

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
    }

    private ParametrosApuestasResponse parameters() {
        return new ParametrosApuestasResponse(
                BigDecimal.valueOf(5000),
                3,
                BigDecimal.valueOf(60000),
                10,
                0,
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
