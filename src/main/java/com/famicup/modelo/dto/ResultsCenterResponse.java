package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoPronostico;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ResultsCenterResponse(
        List<Group> groups) {

    public record Group(
            String groupId,
            String groupName,
            List<Match> matches) {
    }

    public record Match(
            PartidoDto match,
            UserPrediction userPrediction) {
    }

    public record UserPrediction(
            UUID id,
            int predictedHomeGoals,
            int predictedAwayGoals,
            EstadoPronostico status,
            int points,
            boolean exactHit,
            boolean winnerHit,
            OffsetDateTime registeredAt) {
    }
}
