package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoApuestaColombia;
import com.famicup.modelo.enumeracion.EstadoPago;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApuestaColombiaResponse(
        UUID id,
        Long matchId,
        String matchLabel,
        PartidoDto match,
        int predictedHomeGoals,
        int predictedAwayGoals,
        BigDecimal amountCop,
        EstadoApuestaColombia status,
        EstadoPago paymentStatus,
        boolean valid,
        boolean principalGlobalPrediction,
        OffsetDateTime registeredAt,
        BigDecimal prizeAmountCop) {
}
