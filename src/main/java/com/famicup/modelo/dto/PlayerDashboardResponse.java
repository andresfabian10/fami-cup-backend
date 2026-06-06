package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoPago;
import java.util.List;

public record PlayerDashboardResponse(
        int position,
        int totalPlayers,
        int points,
        EstadoPago globalPaymentStatus,
        List<PronosticoGlobalResponse> activePredictions,
        List<PagoResponse> payments) {
}
