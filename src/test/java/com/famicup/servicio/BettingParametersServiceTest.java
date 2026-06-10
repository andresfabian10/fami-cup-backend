package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.famicup.excepcion.ReglaNegocioException;
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
        put(BettingParametersService.GLOBAL_REGISTRATION_AMOUNT, "50000");
        put(BettingParametersService.ORGANIZER_FEE_AMOUNT, "10000");
        put(BettingParametersService.GLOBAL_PRIZE_POOL_AMOUNT, "40000");
        put(BettingParametersService.CLOSING_MINUTES, "10");
        put(BettingParametersService.GLOBAL_EXACT_POINTS, "5");
        put(BettingParametersService.GLOBAL_WINNER_POINTS, "2");
        put(BettingParametersService.GLOBAL_PRIZE_FIRST, "50");
        put(BettingParametersService.GLOBAL_PRIZE_SECOND, "30");
        put(BettingParametersService.GLOBAL_PRIZE_THIRD, "20");
        put(BettingParametersService.GLOBAL_RESERVE, "0");
        put(BettingParametersService.WORLD_CHAMPION_POINTS, "10");
        put(BettingParametersService.WORLD_CHAMPION_LOCK_AT, "2026-06-11T14:00:00");
        put(BettingParametersService.ADMIN_WHATSAPP_NUMBER, "573163353115");
        put(BettingParametersService.FORGOT_PASSWORD_WHATSAPP_MESSAGE, "Hola");
        put(BettingParametersService.REQUEST_ACCESS_WHATSAPP_MESSAGE, "Acceso");
        put(BettingParametersService.FORGOT_PASSWORD_MODAL_TEXT, "Recuperar");
        put(BettingParametersService.REQUEST_ACCESS_MODAL_TEXT, "Solicitar");
        put(BettingParametersService.INTERSTITIAL_BANNER_ENABLED, "false");
        put(BettingParametersService.INTERSTITIAL_BANNER_IMAGE_URL, "");
        put(BettingParametersService.INTERSTITIAL_BANNER_TARGET_URL, "");
        put(BettingParametersService.INTERSTITIAL_BANNER_ALT_TEXT, "Banner");
        put(BettingParametersService.INTERSTITIAL_BANNER_DISMISS_HOURS, "12");
        when(repository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(parameters.get(invocation.getArgument(0))));
    }

    @Test
    void readsBettingParametersFromRepository() {
        var response = service.getParametersNoCache();

        assertThat(response.colombiaBetAmount()).isEqualByComparingTo("5000");
        assertThat(response.globalRegistrationAmount()).isEqualByComparingTo("50000");
        assertThat(response.organizerFeeAmount()).isEqualByComparingTo("10000");
        assertThat(response.globalPrizePoolAmount()).isEqualByComparingTo("40000");
        assertThat(response.colombiaMaxBetsPerMatch()).isEqualTo(3);
        assertThat(response.closingMinutesBeforeMatch()).isEqualTo(10);
        assertThat(response.exactPoints()).isEqualTo(5);
        assertThat(response.globalReservePercent()).isEqualTo(0);
    }

    @Test
    void keepsColombiaRulesFixedWhenUpdatingParameters() {
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        assertThat(parameters.get(BettingParametersService.COLOMBIA_BET_AMOUNT).getParameterValue()).isEqualTo("5000");
        assertThat(parameters.get(BettingParametersService.COLOMBIA_MAX_BETS).getParameterValue()).isEqualTo("3");
        assertThat(parameters.get(BettingParametersService.CLOSING_MINUTES).getParameterValue()).isEqualTo("10");
    }

    @Test
    void returnsOfficialColombiaRulesEvenIfStoredValuesAreStale() {
        parameters.get(BettingParametersService.COLOMBIA_BET_AMOUNT).setParameterValue("10000");
        parameters.get(BettingParametersService.COLOMBIA_MAX_BETS).setParameterValue("1");

        var response = service.getParametersNoCache();

        assertThat(response.colombiaBetAmount()).isEqualByComparingTo("5000");
        assertThat(response.colombiaMaxBetsPerMatch()).isEqualTo(3);
    }

    @Test
    void calculatesPrizePoolFromRegistrationMinusOrganizerFee() {
        var response = service.updateParameters(new ActualizarParametrosRequest(
                null,
                null,
                BigDecimal.valueOf(60000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(40000),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        assertThat(response.globalRegistrationAmount()).isEqualByComparingTo("60000");
        assertThat(response.organizerFeeAmount()).isEqualByComparingTo("10000");
        assertThat(response.globalPrizePoolAmount()).isEqualByComparingTo("50000");
        assertThat(parameters.get(BettingParametersService.GLOBAL_PRIZE_POOL_AMOUNT).getParameterValue()).isEqualTo("50000");
    }

    @Test
    void rejectsOrganizerFeeGreaterThanRegistrationAmount() {
        assertThatThrownBy(() -> service.updateParameters(new ActualizarParametrosRequest(
                null,
                null,
                null,
                BigDecimal.valueOf(60000),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("organizador");
    }

    private void put(String key, String value) {
        ParametroSistema parameter = new ParametroSistema();
        parameter.setParameterKey(key);
        parameter.setParameterValue(value);
        parameter.setValueType(TipoValorParametro.INTEGER);
        parameters.put(key, parameter);
    }
}
