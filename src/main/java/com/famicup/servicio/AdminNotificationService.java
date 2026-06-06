package com.famicup.servicio;

import com.famicup.modelo.dto.AdminNotificationResponse;
import com.famicup.modelo.entidad.ApiSyncLog;
import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.enumeracion.EstadoSincronizacion;
import com.famicup.modelo.enumeracion.EstadoUsuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.repositorio.ApiSyncLogRepository;
import com.famicup.repositorio.PagoRepository;
import com.famicup.repositorio.UsuarioRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminNotificationService {

    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApiSyncLogRepository syncLogRepository;

    public AdminNotificationService(
            PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository,
            ApiSyncLogRepository syncLogRepository) {
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.syncLogRepository = syncLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminNotificationResponse> listNotifications() {
        List<AdminNotificationResponse> notifications = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        long pendingPayments = pagoRepository.countByStatus(EstadoPago.PENDING);
        if (pendingPayments > 0) {
            notifications.add(new AdminNotificationResponse(
                    "pending-payments",
                    "PAYMENTS",
                    "Pagos pendientes",
                    "Hay " + pendingPayments + " pago(s) esperando verificacion.",
                    "warning",
                    "Revisar comprobantes",
                    "/administrador/control-pagos",
                    now));
        }

        long pendingPlayers = usuarioRepository.countByRoleAndStatus(RolUsuario.PLAYER, EstadoUsuario.PENDING);
        if (pendingPlayers > 0) {
            notifications.add(new AdminNotificationResponse(
                    "pending-players",
                    "USERS",
                    "Jugadores pendientes",
                    "Hay " + pendingPlayers + " jugador(es) pendientes de habilitar.",
                    "info",
                    "Gestionar accesos",
                    "/administrador/gestor-usuarios",
                    now));
        }

        for (ApiSyncLog log : syncLogRepository.findTop5ByOrderByStartedAtDesc()) {
            notifications.add(notificationFromSyncLog(log));
        }

        return notifications;
    }

    private AdminNotificationResponse notificationFromSyncLog(ApiSyncLog log) {
        boolean failed = log.getStatus() == EstadoSincronizacion.FAILED;
        String typeLabel = log.getSyncType().name().equals("FIXTURES") ? "fixtures" : "resultados";
        String title = failed ? "Sincronizacion con error" : "Sincronizacion actualizada";
        String message = failed
                ? "La sincronizacion de " + typeLabel + " fallo: " + log.getMessage()
                : "La sincronizacion de " + typeLabel + " proceso " + log.getRecordsProcessed() + " registro(s).";

        return new AdminNotificationResponse(
                "sync-" + log.getId(),
                log.getSyncType().name(),
                title,
                message,
                failed ? "error" : "success",
                failed ? "Reintentar sincronizacion" : null,
                failed ? "/administrador" : null,
                log.getFinishedAt() == null ? log.getStartedAt() : log.getFinishedAt());
    }
}
