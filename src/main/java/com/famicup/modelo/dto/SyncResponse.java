package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoSincronizacion;
import com.famicup.modelo.enumeracion.TipoSincronizacion;
import java.time.OffsetDateTime;

public record SyncResponse(
        TipoSincronizacion type,
        EstadoSincronizacion status,
        int recordsProcessed,
        String message,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt) {
}
