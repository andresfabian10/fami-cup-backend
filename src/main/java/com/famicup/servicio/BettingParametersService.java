package com.famicup.servicio;

import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.modelo.dto.ActualizarParametrosRequest;
import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.modelo.entidad.ParametroSistema;
import com.famicup.repositorio.ParametroSistemaRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BettingParametersService {

    public static final String COLOMBIA_BET_AMOUNT = "COLOMBIA_BET_AMOUNT_COP";
    public static final String COLOMBIA_MAX_BETS = "COLOMBIA_MAX_BETS_PER_MATCH";
    public static final String GLOBAL_REGISTRATION_AMOUNT = "GLOBAL_REGISTRATION_AMOUNT_COP";
    public static final String CLOSING_MINUTES = "CLOSING_MINUTES_BEFORE_MATCH";
    public static final String GLOBAL_EXACT_POINTS = "GLOBAL_EXACT_POINTS";
    public static final String GLOBAL_WINNER_POINTS = "GLOBAL_WINNER_POINTS";
    public static final String GLOBAL_PRIZE_FIRST = "GLOBAL_PRIZE_FIRST_PERCENT";
    public static final String GLOBAL_PRIZE_SECOND = "GLOBAL_PRIZE_SECOND_PERCENT";
    public static final String GLOBAL_PRIZE_THIRD = "GLOBAL_PRIZE_THIRD_PERCENT";
    public static final String GLOBAL_RESERVE = "GLOBAL_RESERVE_PERCENT";
    public static final String WORLD_CHAMPION_POINTS = "WORLD_CHAMPION_POINTS";
    public static final String WORLD_CHAMPION_LOCK_AT = "WORLD_CHAMPION_LOCK_AT";

    private final ParametroSistemaRepository parameterRepository;
    private final AuditService auditService;

    public BettingParametersService(ParametroSistemaRepository parameterRepository, AuditService auditService) {
        this.parameterRepository = parameterRepository;
        this.auditService = auditService;
    }

    @Cacheable("systemParameters")
    @Transactional(readOnly = true)
    public ParametrosApuestasResponse getParameters() {
        return new ParametrosApuestasResponse(
                getDecimal(COLOMBIA_BET_AMOUNT),
                getInt(COLOMBIA_MAX_BETS),
                getDecimal(GLOBAL_REGISTRATION_AMOUNT),
                getInt(CLOSING_MINUTES),
                getInt(GLOBAL_EXACT_POINTS),
                getInt(GLOBAL_WINNER_POINTS),
                getInt(GLOBAL_PRIZE_FIRST),
                getInt(GLOBAL_PRIZE_SECOND),
                getInt(GLOBAL_PRIZE_THIRD),
                getInt(GLOBAL_RESERVE),
                getInt(WORLD_CHAMPION_POINTS),
                getOffsetDateTime(WORLD_CHAMPION_LOCK_AT));
    }

    @CacheEvict(value = "systemParameters", allEntries = true)
    @Transactional
    public ParametrosApuestasResponse updateParameters(ActualizarParametrosRequest request) {
        updateDecimalIfPresent(COLOMBIA_BET_AMOUNT, request.colombiaBetAmount());
        updateIntIfPresent(COLOMBIA_MAX_BETS, request.colombiaMaxBetsPerMatch());
        updateDecimalIfPresent(GLOBAL_REGISTRATION_AMOUNT, request.globalRegistrationAmount());
        updateIntIfPresent(CLOSING_MINUTES, request.closingMinutesBeforeMatch());
        updateIntIfPresent(GLOBAL_EXACT_POINTS, request.exactPoints());
        updateIntIfPresent(GLOBAL_WINNER_POINTS, request.winnerPoints());
        updateIntIfPresent(GLOBAL_PRIZE_FIRST, request.globalPrizeFirstPercent());
        updateIntIfPresent(GLOBAL_PRIZE_SECOND, request.globalPrizeSecondPercent());
        updateIntIfPresent(GLOBAL_PRIZE_THIRD, request.globalPrizeThirdPercent());
        updateIntIfPresent(GLOBAL_RESERVE, request.globalReservePercent());
        auditService.record(null, "PARAMETERS_UPDATE", "SYSTEM_PARAMETERS", "system", "Actualizo parametros del reglamento", "Parametros guardados");
        return getParametersNoCache();
    }

    @Transactional(readOnly = true)
    public ParametrosApuestasResponse getParametersNoCache() {
        return new ParametrosApuestasResponse(
                getDecimal(COLOMBIA_BET_AMOUNT),
                getInt(COLOMBIA_MAX_BETS),
                getDecimal(GLOBAL_REGISTRATION_AMOUNT),
                getInt(CLOSING_MINUTES),
                getInt(GLOBAL_EXACT_POINTS),
                getInt(GLOBAL_WINNER_POINTS),
                getInt(GLOBAL_PRIZE_FIRST),
                getInt(GLOBAL_PRIZE_SECOND),
                getInt(GLOBAL_PRIZE_THIRD),
                getInt(GLOBAL_RESERVE),
                getInt(WORLD_CHAMPION_POINTS),
                getOffsetDateTime(WORLD_CHAMPION_LOCK_AT));
    }

    public int closingMinutesBeforeMatch() {
        return getParameters().closingMinutesBeforeMatch();
    }

    public BigDecimal colombiaBetAmount() {
        return getParameters().colombiaBetAmount();
    }

    public int colombiaMaxBetsPerMatch() {
        return getParameters().colombiaMaxBetsPerMatch();
    }

    public int worldChampionPoints() {
        return getParameters().worldChampionPoints();
    }

    public OffsetDateTime worldChampionLockAt() {
        return getParameters().worldChampionLockAt();
    }

    private int getInt(String key) {
        return Integer.parseInt(getValue(key));
    }

    private BigDecimal getDecimal(String key) {
        return new BigDecimal(getValue(key));
    }

    private OffsetDateTime getOffsetDateTime(String key) {
        String value = getValue(key);
        return value == null || value.isBlank() ? null : OffsetDateTime.parse(value);
    }

    private String getValue(String key) {
        return parameterRepository.findById(key)
                .map(ParametroSistema::getParameterValue)
                .orElseThrow(() -> new RecursoNoEncontradoException("Parametro no encontrado: " + key));
    }

    private void updateIntIfPresent(String key, Integer value) {
        if (value != null) {
            updateValue(key, value.toString());
        }
    }

    private void updateDecimalIfPresent(String key, BigDecimal value) {
        if (value != null) {
            updateValue(key, value.toPlainString());
        }
    }

    private void updateValue(String key, String value) {
        ParametroSistema parameter = parameterRepository.findById(key)
                .orElseThrow(() -> new RecursoNoEncontradoException("Parametro no encontrado: " + key));
        parameter.setParameterValue(value);
    }
}
