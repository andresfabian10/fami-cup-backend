package com.famicup.modelo.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ScoringRecalculationResponse(
        boolean dryRun,
        Long matchId,
        OffsetDateTime from,
        OffsetDateTime to,
        int matchesProcessed,
        int matchesSkipped,
        int predictionsProcessed,
        int predictionsUpdated,
        int predictionsUnchanged,
        int predictionsSkipped,
        int pointsChanged,
        int errorsFound,
        List<String> errors) {
}
