package com.famicup.modelo.dto;

import java.util.List;

public record HistorialJugadorResponse(
        List<ApuestaColombiaResponse> colombiaBets,
        List<PronosticoGlobalResponse> globalPredictions) {
}
