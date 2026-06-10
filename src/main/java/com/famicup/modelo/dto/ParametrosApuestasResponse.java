package com.famicup.modelo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ParametrosApuestasResponse(
        BigDecimal colombiaBetAmount,
        int colombiaMaxBetsPerMatch,
        BigDecimal globalRegistrationAmount,
        BigDecimal organizerFeeAmount,
        BigDecimal globalPrizePoolAmount,
        int closingMinutesBeforeMatch,
        int exactPoints,
        int winnerPoints,
        int globalPrizeFirstPercent,
        int globalPrizeSecondPercent,
        int globalPrizeThirdPercent,
        int globalReservePercent,
        int worldChampionPoints,
        OffsetDateTime worldChampionLockAt,
        String adminWhatsappNumber,
        String forgotPasswordWhatsappMessage,
        String requestAccessWhatsappMessage,
        String forgotPasswordModalText,
        String requestAccessModalText,
        boolean interstitialBannerEnabled,
        String interstitialBannerImageUrl,
        String interstitialBannerTargetUrl,
        String interstitialBannerAltText,
        int interstitialBannerDismissHours) {
}
