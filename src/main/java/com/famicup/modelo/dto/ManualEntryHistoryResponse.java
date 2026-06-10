package com.famicup.modelo.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ManualEntryHistoryResponse(
        UUID userId,
        String username,
        String fullName,
        List<Item> items) {

    public record Item(
            UUID id,
            String modality,
            PartidoDto match,
            Integer predictedHomeGoals,
            Integer predictedAwayGoals,
            String predictionLabel,
            boolean principalGlobalPrediction,
            String entryOrigin,
            String createdByAdminUsername,
            String updatedByAdminUsername,
            String status,
            OffsetDateTime registeredAt,
            OffsetDateTime updatedAt) {
    }
}
