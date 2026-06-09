package com.famicup.modelo.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminPredictionsResponse(
        Summary summary,
        List<Item> items) {

    public record Summary(
            int totalPredictions,
            int playersWithPredictions,
            int colombiaPredictions,
            int globalPredictions,
            int championPredictions,
            int principalColombiaPredictions,
            String topMatchLabel,
            int topMatchCount) {
    }

    public record Player(
            UUID id,
            String username,
            String fullName) {
    }

    public record Item(
            UUID id,
            String modality,
            Player player,
            PartidoDto match,
            EquipoDto championTeam,
            Integer predictedHomeGoals,
            Integer predictedAwayGoals,
            String predictionLabel,
            String resultLabel,
            boolean principalGlobalPrediction,
            String rawStatus,
            String adminStatus,
            int points,
            int possiblePoints,
            OffsetDateTime registeredAt,
            OffsetDateTime updatedAt) {
    }
}
