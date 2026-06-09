package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.famicup.modelo.entidad.Equipo;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.ResultadoPartido;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPartido;
import com.famicup.modelo.enumeracion.EstadoPronostico;
import com.famicup.modelo.enumeracion.GanadorPartido;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.repositorio.PartidoRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import com.famicup.repositorio.ResultadoPartidoRepository;
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
class ResultsCenterServiceTest {

    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private PronosticoGlobalRepository predictionRepository;
    @Mock
    private ResultadoPartidoRepository resultRepository;

    private ResultsCenterService service;

    @BeforeEach
    void setUp() {
        service = new ResultsCenterService(
                partidoRepository,
                predictionRepository,
                new PartidoMapper(resultRepository));
    }

    @Test
    void groupsMatchesWithSpanishTeamNamesAndUserPrediction() {
        Usuario user = new Usuario();
        user.setId(UUID.randomUUID());
        Partido groupMatch = match(1L, "Group A", team("CAN", "Canada"), team("MEX", "Mexico"), EstadoPartido.FINISHED);
        Partido matchdayMatch = match(2L, "Group Stage - 1", team("GER", "Germany"), team("USA", "United States"), EstadoPartido.SCHEDULED);
        PronosticoGlobal prediction = prediction(user, groupMatch);
        ResultadoPartido result = result(groupMatch);

        when(partidoRepository.findAllWithTeamsOrderByKickoffAtUtcAsc()).thenReturn(List.of(groupMatch, matchdayMatch));
        when(predictionRepository.findByUserAndMatchIn(user, List.of(groupMatch, matchdayMatch))).thenReturn(List.of(prediction));
        when(resultRepository.findByMatchId(1L)).thenReturn(Optional.of(result));
        when(resultRepository.findByMatchId(2L)).thenReturn(Optional.empty());

        var response = service.getResultsCenter(user);

        assertThat(response.groups()).hasSize(2);
        assertThat(response.groups().getFirst().groupName()).isEqualTo("Grupo A");
        assertThat(response.groups().getFirst().matches().getFirst().match().homeTeam().displayName()).isEqualTo("Canadá");
        assertThat(response.groups().getFirst().matches().getFirst().match().awayTeam().displayName()).isEqualTo("México");
        assertThat(response.groups().getFirst().matches().getFirst().match().result().homeGoals90()).isEqualTo(1);
        assertThat(response.groups().getFirst().matches().getFirst().userPrediction().predictedHomeGoals()).isEqualTo(1);
        assertThat(response.groups().getFirst().matches().getFirst().userPrediction().points()).isEqualTo(5);
        assertThat(response.groups().get(1).groupName()).isEqualTo("Fase de grupos - Fecha 1");
        assertThat(response.groups().get(1).matches().getFirst().match().homeTeam().displayName()).isEqualTo("Alemania");
        assertThat(response.groups().get(1).matches().getFirst().match().awayTeam().displayName()).isEqualTo("Estados Unidos");
    }

    private Partido match(Long id, String groupName, Equipo home, Equipo away, EstadoPartido status) {
        Partido match = new Partido();
        match.setId(id);
        match.setGroupName(groupName);
        match.setStage(groupName);
        match.setRoundName(groupName);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setStatus(status);
        match.setKickoffAtUtc(OffsetDateTime.parse("2030-06-12T20:00:00Z").plusDays(id));
        return match;
    }

    private Equipo team(String code, String name) {
        Equipo team = new Equipo();
        team.setFifaCode(code);
        team.setName(name);
        team.setFlagUrl("/flags/" + code.toLowerCase() + ".svg");
        return team;
    }

    private PronosticoGlobal prediction(Usuario user, Partido match) {
        PronosticoGlobal prediction = new PronosticoGlobal();
        prediction.setId(UUID.randomUUID());
        prediction.setUser(user);
        prediction.setMatch(match);
        prediction.setPredictedHomeGoals(1);
        prediction.setPredictedAwayGoals(2);
        prediction.setStatus(EstadoPronostico.EVALUATED);
        prediction.setPoints(5);
        prediction.setExactHit(true);
        prediction.setRegisteredAt(OffsetDateTime.parse("2030-06-10T20:00:00Z"));
        return prediction;
    }

    private ResultadoPartido result(Partido match) {
        ResultadoPartido result = new ResultadoPartido();
        result.setMatch(match);
        result.setHomeGoals90(1);
        result.setAwayGoals90(2);
        result.setWinner90(GanadorPartido.AWAY);
        result.setConfirmedAt(OffsetDateTime.parse("2030-06-12T22:00:00Z"));
        return result;
    }
}
