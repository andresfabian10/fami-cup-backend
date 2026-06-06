package com.famicup.modelo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record CrearApuestasColombiaRequest(
        @NotNull Long matchId,
        @NotEmpty List<@Valid MarcadorRequest> bets) {

    public record MarcadorRequest(
            @NotNull @PositiveOrZero Integer homeGoals,
            @NotNull @PositiveOrZero Integer awayGoals,
            Boolean principalGlobalPrediction) {
    }
}
