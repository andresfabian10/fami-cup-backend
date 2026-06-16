package com.famicup.modelo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record GuardarResultadoManualRequest(
        @NotNull @PositiveOrZero Integer homeGoals90,
        @NotNull @PositiveOrZero Integer awayGoals90) {
}
