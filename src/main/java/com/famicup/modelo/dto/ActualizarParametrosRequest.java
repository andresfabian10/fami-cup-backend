package com.famicup.modelo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ActualizarParametrosRequest(
        @Min(1) BigDecimal colombiaBetAmount,
        @Min(1) @Max(20) Integer colombiaMaxBetsPerMatch,
        @Min(1) BigDecimal globalRegistrationAmount,
        @Min(0) BigDecimal organizerFeeAmount,
        @Min(0) BigDecimal globalPrizePoolAmount,
        @Min(0) @Max(180) Integer closingMinutesBeforeMatch,
        @Min(1) @Max(20) Integer exactPoints,
        @Min(1) @Max(20) Integer winnerPoints,
        @Min(0) @Max(100) Integer globalPrizeFirstPercent,
        @Min(0) @Max(100) Integer globalPrizeSecondPercent,
        @Min(0) @Max(100) Integer globalPrizeThirdPercent,
        @Min(0) @Max(100) Integer globalReservePercent,
        @Size(max = 50) String worldChampionLockAt,
        @Size(max = 20) String adminWhatsappNumber,
        @Size(max = 500) String forgotPasswordWhatsappMessage,
        @Size(max = 500) String requestAccessWhatsappMessage,
        @Size(max = 800) String forgotPasswordModalText,
        @Size(max = 800) String requestAccessModalText,
        Boolean interstitialBannerEnabled,
        @Size(max = 1000) String interstitialBannerImageUrl,
        @Size(max = 1000) String interstitialBannerTargetUrl,
        @Size(max = 250) String interstitialBannerAltText,
        @Min(0) @Max(720) Integer interstitialBannerDismissHours) {
}
