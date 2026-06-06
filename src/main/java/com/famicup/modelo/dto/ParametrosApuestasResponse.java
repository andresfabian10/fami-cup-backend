package com.famicup.modelo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ParametrosApuestasResponse(
        BigDecimal colombiaBetAmount,
        int colombiaMaxBetsPerMatch,
        BigDecimal globalRegistrationAmount,
        int closingMinutesBeforeMatch,
        int exactPoints,
        int winnerPoints,
        int globalPrizeFirstPercent,
        int globalPrizeSecondPercent,
        int globalPrizeThirdPercent,
        int globalReservePercent,
        int worldChampionPoints,
        OffsetDateTime worldChampionLockAt) {
}
