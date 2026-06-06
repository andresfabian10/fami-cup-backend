package com.famicup.modelo.dto;

import java.time.OffsetDateTime;

public record AdminNotificationResponse(
        String id,
        String type,
        String title,
        String message,
        String severity,
        String actionLabel,
        String actionPath,
        OffsetDateTime createdAt) {
}
