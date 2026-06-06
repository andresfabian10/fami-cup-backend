package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoCampeonMundial;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PronosticoCampeonMundialResponse(
        UUID id,
        UUID userId,
        String username,
        String fullName,
        EquipoDto team,
        EstadoCampeonMundial status,
        int points,
        int pointsIfCorrect,
        boolean editable,
        OffsetDateTime lockAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
