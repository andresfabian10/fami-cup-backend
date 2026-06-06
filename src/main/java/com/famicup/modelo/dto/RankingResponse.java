package com.famicup.modelo.dto;

import java.util.UUID;

public record RankingResponse(
        UUID userId,
        int position,
        String fullName,
        String username,
        String initials,
        int points,
        int exactHits,
        int winners,
        int colombiaPoints,
        boolean currentUser) {
}
