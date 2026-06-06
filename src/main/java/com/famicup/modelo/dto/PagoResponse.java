package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.enumeracion.SistemaPago;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PagoResponse(
        UUID id,
        UUID userId,
        String username,
        String fullName,
        SistemaPago system,
        Long matchId,
        UUID colombiaBetId,
        BigDecimal amountCop,
        EstadoPago status,
        String paymentMethod,
        String reference,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt) {
}
