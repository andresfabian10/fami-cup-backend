package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.famicup.modelo.dto.ActualizarParametrosRequest;
import com.famicup.modelo.entidad.ParametroSistema;
import com.famicup.modelo.enumeracion.TipoValorParametro;
import com.famicup.repositorio.ParametroSistemaRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BettingParametersServiceTest {

    @Mock
    private ParametroSistemaRepository repository;
    @Mock
    private AuditService auditService;

    private final Map<String, ParametroSistema> parameters = new HashMap<>();
    private BettingParametersService service;

    @BeforeEach
    void setUp() {
        service = new BettingParametersService(repository, auditService);
        put(BettingParametersService.COLOMBIA_BET_AMOUNT, "5000");
        put(BettingParametersService.COLOMBIA_MAX_BETS, "3");
        put(BettingParametersService.GLOBAL_REGISTRATION_AMOUNT, "20000");
        put(BettingParametersService.CLOSING_MINUTES, "10");
        put(BettingParametersService.GLOBAL_EXACT_POINTS, "5");
        put(BettingParametersService.GLOBAL_WINNER_POINTS, "2");
        put(BettingParametersService.GLOBAL_PRIZE_FIRST, "50");
        put(BettingParametersService.GLOBAL_PRIZE_SECOND, "30");
        put(BettingParametersService.GLOBAL_PRIZE_THIRD, "20");
        put(BettingParametersService.GLOBAL_RESERVE, "5");
        put(BettingParametersService.WORLD_CHAMPION_POINTS, "10");
        put(BettingParametersService.WORLD_CHAMPION_LOCK_AT, "2026-06-11T00:00:00Z");
        when(repository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(parameters.get(invocation.getArgument(0))));
    }

    @Test
    void readsBettingParametersFromRepository() {
        var response = service.getParametersNoCache();

        assertThat(response.colombiaBetAmount()).isEqualByComparingTo("5000");
        assertThat(response.colombiaMaxBetsPerMatch()).isEqualTo(3);
        assertThat(response.closingMinutesBeforeMatch()).isEqualTo(10);
        assertThat(response.exactPoints()).isEqualTo(5);
    }

    @Test
    void updatesOnlyProvidedParameters() {
        service.updateParameters(new ActualizarParametrosRequest(
                BigDecimal.valueOf(7000),
                4,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        assertThat(parameters.get(BettingParametersService.COLOMBIA_BET_AMOUNT).getParameterValue()).isEqualTo("7000");
        assertThat(parameters.get(BettingParametersService.COLOMBIA_MAX_BETS).getParameterValue()).isEqualTo("4");
        assertThat(parameters.get(BettingParametersService.CLOSING_MINUTES).getParameterValue()).isEqualTo("10");
    }

    private void put(String key, String value) {
        ParametroSistema parameter = new ParametroSistema();
        parameter.setParameterKey(key);
        parameter.setParameterValue(value);
        parameter.setValueType(TipoValorParametro.INTEGER);
        parameters.put(key, parameter);
    }
}
