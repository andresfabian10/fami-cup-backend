package com.famicup.modelo.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalPlayers,
        long activePlayers,
        long paidPayments,
        long pendingPayments,
        BigDecimal totalCollected,
        BigDecimal globalCollected,
        BigDecimal colombiaCollected,
        List<PartidoDto> upcomingMatches,
        List<RankingResponse> ranking) {
}
