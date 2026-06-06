package com.famicup.modelo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record GuardarPronosticoGlobalRequest(
        @NotNull Long matchId,
        @NotNull @PositiveOrZero Integer homeGoals,
        @NotNull @PositiveOrZero Integer awayGoals) {
}
