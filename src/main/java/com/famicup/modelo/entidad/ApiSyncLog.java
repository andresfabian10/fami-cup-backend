package com.famicup.modelo.entidad;

import com.famicup.modelo.enumeracion.EstadoSincronizacion;
import com.famicup.modelo.enumeracion.TipoSincronizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "api_sync_logs")
public class ApiSyncLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false, length = 40)
    private TipoSincronizacion syncType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSincronizacion status;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "records_processed", nullable = false)
    private Integer recordsProcessed = 0;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;
}
