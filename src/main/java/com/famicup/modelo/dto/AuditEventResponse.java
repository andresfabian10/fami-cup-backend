package com.famicup.modelo.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID userId,
        String username,
        String role,
        String action,
        String entityType,
        String entityId,
        String requestSummary,
        String responseSummary,
        String ipAddress,
        String userAgent,
        OffsetDateTime createdAt) {
}
