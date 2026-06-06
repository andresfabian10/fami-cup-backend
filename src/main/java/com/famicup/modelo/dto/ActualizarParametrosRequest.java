package com.famicup.modelo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record ActualizarParametrosRequest(
        @Min(1) BigDecimal colombiaBetAmount,
        @Min(1) @Max(20) Integer colombiaMaxBetsPerMatch,
        @Min(1) BigDecimal globalRegistrationAmount,
        @Min(1) @Max(180) Integer closingMinutesBeforeMatch,
        @Min(1) @Max(20) Integer exactPoints,
        @Min(1) @Max(20) Integer winnerPoints,
        @Min(0) @Max(100) Integer globalPrizeFirstPercent,
        @Min(0) @Max(100) Integer globalPrizeSecondPercent,
        @Min(0) @Max(100) Integer globalPrizeThirdPercent,
        @Min(0) @Max(100) Integer globalReservePercent) {
}
