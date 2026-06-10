package com.famicup.servicio;

import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.dto.ActualizarParametrosRequest;
import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.modelo.entidad.ParametroSistema;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.repositorio.ParametroSistemaRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BettingParametersService {

    public static final String COLOMBIA_BET_AMOUNT = "COLOMBIA_BET_AMOUNT_COP";
    public static final String COLOMBIA_MAX_BETS = "COLOMBIA_MAX_BETS_PER_MATCH";
    public static final String GLOBAL_REGISTRATION_AMOUNT = "GLOBAL_REGISTRATION_AMOUNT_COP";
    public static final String ORGANIZER_FEE_AMOUNT = "ORGANIZER_FEE_AMOUNT_COP";
    public static final String GLOBAL_PRIZE_POOL_AMOUNT = "GLOBAL_PRIZE_POOL_AMOUNT_COP";
    public static final String CLOSING_MINUTES = "CLOSING_MINUTES_BEFORE_MATCH";
    public static final String GLOBAL_EXACT_POINTS = "GLOBAL_EXACT_POINTS";
    public static final String GLOBAL_WINNER_POINTS = "GLOBAL_WINNER_POINTS";
    public static final String GLOBAL_PRIZE_FIRST = "GLOBAL_PRIZE_FIRST_PERCENT";
    public static final String GLOBAL_PRIZE_SECOND = "GLOBAL_PRIZE_SECOND_PERCENT";
    public static final String GLOBAL_PRIZE_THIRD = "GLOBAL_PRIZE_THIRD_PERCENT";
    public static final String GLOBAL_RESERVE = "GLOBAL_RESERVE_PERCENT";
    public static final String WORLD_CHAMPION_POINTS = "WORLD_CHAMPION_POINTS";
    public static final String WORLD_CHAMPION_LOCK_AT = "WORLD_CHAMPION_LOCK_AT";
    public static final String ADMIN_WHATSAPP_NUMBER = "ADMIN_WHATSAPP_NUMBER";
    public static final String FORGOT_PASSWORD_WHATSAPP_MESSAGE = "FORGOT_PASSWORD_WHATSAPP_MESSAGE";
    public static final String REQUEST_ACCESS_WHATSAPP_MESSAGE = "REQUEST_ACCESS_WHATSAPP_MESSAGE";
    public static final String FORGOT_PASSWORD_MODAL_TEXT = "FORGOT_PASSWORD_MODAL_TEXT";
    public static final String REQUEST_ACCESS_MODAL_TEXT = "REQUEST_ACCESS_MODAL_TEXT";
    public static final String INTERSTITIAL_BANNER_ENABLED = "INTERSTITIAL_BANNER_ENABLED";
    public static final String INTERSTITIAL_BANNER_IMAGE_URL = "INTERSTITIAL_BANNER_IMAGE_URL";
    public static final String INTERSTITIAL_BANNER_TARGET_URL = "INTERSTITIAL_BANNER_TARGET_URL";
    public static final String INTERSTITIAL_BANNER_ALT_TEXT = "INTERSTITIAL_BANNER_ALT_TEXT";
    public static final String INTERSTITIAL_BANNER_DISMISS_HOURS = "INTERSTITIAL_BANNER_DISMISS_HOURS";
    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

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
                getDecimal(ORGANIZER_FEE_AMOUNT),
                getDecimal(GLOBAL_PRIZE_POOL_AMOUNT),
                getInt(CLOSING_MINUTES),
                getInt(GLOBAL_EXACT_POINTS),
                getInt(GLOBAL_WINNER_POINTS),
                getInt(GLOBAL_PRIZE_FIRST),
                getInt(GLOBAL_PRIZE_SECOND),
                getInt(GLOBAL_PRIZE_THIRD),
                getInt(GLOBAL_RESERVE),
                getInt(WORLD_CHAMPION_POINTS),
                getOffsetDateTime(WORLD_CHAMPION_LOCK_AT),
                getText(ADMIN_WHATSAPP_NUMBER),
                getText(FORGOT_PASSWORD_WHATSAPP_MESSAGE),
                getText(REQUEST_ACCESS_WHATSAPP_MESSAGE),
                getText(FORGOT_PASSWORD_MODAL_TEXT),
                getText(REQUEST_ACCESS_MODAL_TEXT),
                getBoolean(INTERSTITIAL_BANNER_ENABLED),
                getText(INTERSTITIAL_BANNER_IMAGE_URL),
                getText(INTERSTITIAL_BANNER_TARGET_URL),
                getText(INTERSTITIAL_BANNER_ALT_TEXT),
                getInt(INTERSTITIAL_BANNER_DISMISS_HOURS));
    }

    @CacheEvict(value = "systemParameters", allEntries = true)
    @Transactional
    public ParametrosApuestasResponse updateParameters(ActualizarParametrosRequest request) {
        return updateParameters(request, null);
    }

    @CacheEvict(value = "systemParameters", allEntries = true)
    @Transactional
    public ParametrosApuestasResponse updateParameters(ActualizarParametrosRequest request, Usuario admin) {
        updateDecimalIfPresent(COLOMBIA_BET_AMOUNT, request.colombiaBetAmount());
        updateIntIfPresent(COLOMBIA_MAX_BETS, request.colombiaMaxBetsPerMatch());
        updateDecimalIfPresent(GLOBAL_REGISTRATION_AMOUNT, request.globalRegistrationAmount());
        updateDecimalIfPresent(ORGANIZER_FEE_AMOUNT, request.organizerFeeAmount());
        updateDecimalIfPresent(GLOBAL_PRIZE_POOL_AMOUNT, request.globalPrizePoolAmount());
        updateIntIfPresent(CLOSING_MINUTES, request.closingMinutesBeforeMatch());
        updateIntIfPresent(GLOBAL_EXACT_POINTS, request.exactPoints());
        updateIntIfPresent(GLOBAL_WINNER_POINTS, request.winnerPoints());
        updateIntIfPresent(GLOBAL_PRIZE_FIRST, request.globalPrizeFirstPercent());
        updateIntIfPresent(GLOBAL_PRIZE_SECOND, request.globalPrizeSecondPercent());
        updateIntIfPresent(GLOBAL_PRIZE_THIRD, request.globalPrizeThirdPercent());
        updateIntIfPresent(GLOBAL_RESERVE, request.globalReservePercent());
        updateWorldChampionLockAtIfPresent(request.worldChampionLockAt());
        updateTextIfPresent(ADMIN_WHATSAPP_NUMBER, request.adminWhatsappNumber());
        updateTextIfPresent(FORGOT_PASSWORD_WHATSAPP_MESSAGE, request.forgotPasswordWhatsappMessage());
        updateTextIfPresent(REQUEST_ACCESS_WHATSAPP_MESSAGE, request.requestAccessWhatsappMessage());
        updateTextIfPresent(FORGOT_PASSWORD_MODAL_TEXT, request.forgotPasswordModalText());
        updateTextIfPresent(REQUEST_ACCESS_MODAL_TEXT, request.requestAccessModalText());
        updateBooleanIfPresent(INTERSTITIAL_BANNER_ENABLED, request.interstitialBannerEnabled());
        updateTextIfPresent(INTERSTITIAL_BANNER_IMAGE_URL, request.interstitialBannerImageUrl());
        updateTextIfPresent(INTERSTITIAL_BANNER_TARGET_URL, request.interstitialBannerTargetUrl());
        updateTextIfPresent(INTERSTITIAL_BANNER_ALT_TEXT, request.interstitialBannerAltText());
        updateIntIfPresent(INTERSTITIAL_BANNER_DISMISS_HOURS, request.interstitialBannerDismissHours());
        auditService.record(admin, "PARAMETERS_UPDATE", "SYSTEM_PARAMETERS", "system", "Actualizo parametros del reglamento", "Parametros guardados");
        return getParametersNoCache();
    }

    @Transactional(readOnly = true)
    public ParametrosApuestasResponse getParametersNoCache() {
        return new ParametrosApuestasResponse(
                getDecimal(COLOMBIA_BET_AMOUNT),
                getInt(COLOMBIA_MAX_BETS),
                getDecimal(GLOBAL_REGISTRATION_AMOUNT),
                getDecimal(ORGANIZER_FEE_AMOUNT),
                getDecimal(GLOBAL_PRIZE_POOL_AMOUNT),
                getInt(CLOSING_MINUTES),
                getInt(GLOBAL_EXACT_POINTS),
                getInt(GLOBAL_WINNER_POINTS),
                getInt(GLOBAL_PRIZE_FIRST),
                getInt(GLOBAL_PRIZE_SECOND),
                getInt(GLOBAL_PRIZE_THIRD),
                getInt(GLOBAL_RESERVE),
                getInt(WORLD_CHAMPION_POINTS),
                getOffsetDateTime(WORLD_CHAMPION_LOCK_AT),
                getText(ADMIN_WHATSAPP_NUMBER),
                getText(FORGOT_PASSWORD_WHATSAPP_MESSAGE),
                getText(REQUEST_ACCESS_WHATSAPP_MESSAGE),
                getText(FORGOT_PASSWORD_MODAL_TEXT),
                getText(REQUEST_ACCESS_MODAL_TEXT),
                getBoolean(INTERSTITIAL_BANNER_ENABLED),
                getText(INTERSTITIAL_BANNER_IMAGE_URL),
                getText(INTERSTITIAL_BANNER_TARGET_URL),
                getText(INTERSTITIAL_BANNER_ALT_TEXT),
                getInt(INTERSTITIAL_BANNER_DISMISS_HOURS));
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

    public BigDecimal globalRegistrationAmount() {
        return getParameters().globalRegistrationAmount();
    }

    private int getInt(String key) {
        return Integer.parseInt(getValue(key));
    }

    private BigDecimal getDecimal(String key) {
        return new BigDecimal(getValue(key));
    }

    private boolean getBoolean(String key) {
        return Boolean.parseBoolean(getValue(key));
    }

    private String getText(String key) {
        return getValue(key);
    }

    private OffsetDateTime getOffsetDateTime(String key) {
        String value = getValue(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (java.time.format.DateTimeParseException ignored) {
            return LocalDateTime.parse(value).atZone(BOGOTA_ZONE).toOffsetDateTime();
        }
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

    private void updateBooleanIfPresent(String key, Boolean value) {
        if (value != null) {
            updateValue(key, value.toString());
        }
    }

    private void updateTextIfPresent(String key, String value) {
        if (value != null) {
            updateValue(key, value.trim());
        }
    }

    private void updateWorldChampionLockAtIfPresent(String value) {
        if (value == null) {
            return;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.isBlank()) {
            throw new ReglaNegocioException("La fecha de cierre de campeon mundial no puede estar vacia.");
        }
        getOffsetDateTimeFromValue(normalizedValue);
        updateValue(WORLD_CHAMPION_LOCK_AT, normalizedValue);
    }

    private OffsetDateTime getOffsetDateTimeFromValue(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (java.time.format.DateTimeParseException ignored) {
            return LocalDateTime.parse(value).atZone(BOGOTA_ZONE).toOffsetDateTime();
        }
    }

    private void updateValue(String key, String value) {
        ParametroSistema parameter = parameterRepository.findById(key)
                .orElseThrow(() -> new RecursoNoEncontradoException("Parametro no encontrado: " + key));
        parameter.setParameterValue(value);
    }
}
