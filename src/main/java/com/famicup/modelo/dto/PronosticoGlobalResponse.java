package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoPronostico;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PronosticoGlobalResponse(
        UUID id,
        PartidoDto match,
        int predictedHomeGoals,
        int predictedAwayGoals,
        EstadoPronostico status,
        int points,
        boolean exactHit,
        boolean winnerHit,
        String entryOrigin,
        String createdByAdminUsername,
        String updatedByAdminUsername,
        OffsetDateTime registeredAt) {
}
