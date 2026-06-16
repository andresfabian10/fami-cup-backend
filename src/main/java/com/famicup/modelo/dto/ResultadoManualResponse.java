package com.famicup.modelo.dto;

public record ResultadoManualResponse(
        PartidoDto match,
        ResultadoPartidoDto result,
        ScoringRecalculationResponse recalculation) {
}
