package com.famicup.controlador;

import com.famicup.modelo.dto.ActualizarParametrosRequest;
import com.famicup.modelo.dto.ActualizarApuestaColombiaRequest;
import com.famicup.modelo.dto.ActualizarPronosticoGlobalManualRequest;
import com.famicup.modelo.dto.ActualizarUsuarioRequest;
import com.famicup.modelo.dto.ApuestaColombiaResponse;
import com.famicup.modelo.dto.AuditEventResponse;
import com.famicup.modelo.dto.AdminNotificationResponse;
import com.famicup.modelo.dto.AdminDashboardResponse;
import com.famicup.modelo.dto.AdminPredictionsResponse;
import com.famicup.modelo.dto.CrearApuestasColombiaRequest;
import com.famicup.modelo.dto.CrearUsuarioRequest;
import com.famicup.modelo.dto.GuardarPronosticoGlobalRequest;
import com.famicup.modelo.dto.GuardarResultadoManualRequest;
import com.famicup.modelo.dto.ManualEntryHistoryResponse;
import com.famicup.modelo.dto.PagoResponse;
import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.modelo.dto.PartidoDto;
import com.famicup.modelo.dto.PronosticoCampeonMundialResponse;
import com.famicup.modelo.dto.PronosticoGlobalResponse;
import com.famicup.modelo.dto.ResultadoManualResponse;
import com.famicup.modelo.dto.ScoringRecalculationResponse;
import com.famicup.modelo.dto.SyncResponse;
import com.famicup.modelo.dto.UsuarioResponse;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.servicio.ApiFootballSyncService;
import com.famicup.servicio.AdminNotificationService;
import com.famicup.servicio.AdminManualEntryService;
import com.famicup.servicio.AdminPredictionsService;
import com.famicup.servicio.AuditExportService;
import com.famicup.servicio.AuditService;
import com.famicup.servicio.BettingParametersService;
import com.famicup.servicio.DashboardService;
import com.famicup.servicio.EvidenceExportService;
import com.famicup.servicio.ManualResultService;
import com.famicup.servicio.PagoService;
import com.famicup.servicio.PredictionScoringService;
import com.famicup.servicio.UsuarioService;
import com.famicup.servicio.WorldChampionPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Admin", description = "Operaciones administrativas protegidas para ADMIN.")
public class AdminController {

    private final DashboardService dashboardService;
    private final UsuarioService usuarioService;
    private final PagoService pagoService;
    private final BettingParametersService parametersService;
    private final ApiFootballSyncService syncService;
    private final AdminNotificationService notificationService;
    private final AdminPredictionsService predictionsService;
    private final AdminManualEntryService manualEntryService;
    private final WorldChampionPredictionService championPredictionService;
    private final AuditService auditService;
    private final AuditExportService auditExportService;
    private final EvidenceExportService evidenceExportService;
    private final PredictionScoringService scoringService;
    private final ManualResultService manualResultService;

    public AdminController(
            DashboardService dashboardService,
            UsuarioService usuarioService,
            PagoService pagoService,
            BettingParametersService parametersService,
            ApiFootballSyncService syncService,
            AdminNotificationService notificationService,
            AdminPredictionsService predictionsService,
            AdminManualEntryService manualEntryService,
            WorldChampionPredictionService championPredictionService,
            AuditService auditService,
            AuditExportService auditExportService,
            EvidenceExportService evidenceExportService,
            PredictionScoringService scoringService,
            ManualResultService manualResultService) {
        this.dashboardService = dashboardService;
        this.usuarioService = usuarioService;
        this.pagoService = pagoService;
        this.parametersService = parametersService;
        this.syncService = syncService;
        this.notificationService = notificationService;
        this.predictionsService = predictionsService;
        this.manualEntryService = manualEntryService;
        this.championPredictionService = championPredictionService;
        this.auditService = auditService;
        this.auditExportService = auditExportService;
        this.evidenceExportService = evidenceExportService;
        this.scoringService = scoringService;
        this.manualResultService = manualResultService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard administrador", description = "Resumen de participantes, pagos, proximos partidos y top de ranking. Rol permitido: ADMIN.")
    public AdminDashboardResponse dashboard() {
        return dashboardService.adminDashboard();
    }

    @GetMapping("/notifications")
    @Operation(summary = "Notificaciones administrativas", description = "Lista alertas generadas desde pagos, usuarios y sincronizaciones. Rol permitido: ADMIN.")
    public List<AdminNotificationResponse> notifications() {
        return notificationService.listNotifications();
    }

    @GetMapping("/users")
    @Operation(summary = "Listar usuarios", description = "Lista usuarios registrados sin exponer hashes ni datos sensibles. Rol permitido: ADMIN.")
    public List<UsuarioResponse> listUsers() {
        return usuarioService.listUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear usuario", description = "Crea un ADMIN o PLAYER. La regla de negocio impide mas de un ADMIN.")
    public UsuarioResponse createUser(@Valid @RequestBody CrearUsuarioRequest request) {
        return usuarioService.createUser(request);
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza datos editables, estado o contraseña de un usuario.")
    public UsuarioResponse updateUser(@PathVariable UUID id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        return usuarioService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar usuario", description = "Elimina usuario siempre que no sea el unico ADMIN.")
    public void deleteUser(@PathVariable UUID id) {
        usuarioService.deleteUser(id);
    }

    @GetMapping("/payments")
    @Operation(summary = "Listar pagos", description = "Lista pagos globales y Colombia Especial para validacion administrativa.")
    public List<PagoResponse> listPayments() {
        return pagoService.listPayments();
    }

    @PutMapping("/payments/{id}/mark-paid")
    @Operation(summary = "Marcar pago confirmado", description = "Marca un pago como PAID. En apuestas Colombia valida la apuesta solo si aun no cerro el partido.")
    public PagoResponse markPaid(@PathVariable UUID id, Authentication authentication) {
        Usuario admin = usuarioService.getCurrentUser(authentication);
        return pagoService.markPaid(id, admin);
    }

    @PutMapping("/payments/{id}/mark-pending")
    @Operation(summary = "Marcar pago pendiente", description = "Devuelve un pago a PENDING y deja apuestas Colombia asociadas pendientes de pago.")
    public PagoResponse markPending(@PathVariable UUID id) {
        return pagoService.markPending(id);
    }

    @GetMapping("/parameters")
    @Operation(summary = "Consultar parametros", description = "Obtiene montos, limites, cierre y puntajes vigentes del reglamento.")
    public ParametrosApuestasResponse parameters() {
        return parametersService.getParameters();
    }

    @PutMapping("/parameters")
    @Operation(summary = "Actualizar parametros", description = "Actualiza montos, limites, cierre, puntos y porcentajes. Rol permitido: ADMIN.")
    public ParametrosApuestasResponse updateParameters(@Valid @RequestBody ActualizarParametrosRequest request, Authentication authentication) {
        return parametersService.updateParameters(request, usuarioService.getCurrentUser(authentication));
    }

    @PostMapping("/sync/fixtures")
    @Operation(summary = "Sincronizar fixtures", description = "Consulta API-Football y guarda equipos, competencia y partidos en PostgreSQL. No se usa para cada request del frontend.")
    public SyncResponse syncFixtures() {
        return syncService.syncFixtures();
    }

    @PostMapping("/sync/results")
    @Operation(summary = "Sincronizar resultados", description = "Actualiza solo partidos relevantes y recalcula pronosticos/ranking cuando hay resultados finales.")
    public SyncResponse syncResults() {
        return syncService.syncResults();
    }

    @PostMapping("/scoring/recalculate")
    @Operation(summary = "Recalcular puntos", description = "Recalcula puntos de Polla Global de forma idempotente. Permite matchId o rango de fechas y dryRun.")
    public ScoringRecalculationResponse recalculateScoring(
            Authentication authentication,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return scoringService.recalculate(usuarioService.getCurrentUser(authentication), matchId, from, to, dryRun);
    }

    @GetMapping("/world-champion-predictions")
    @Operation(summary = "Pronosticos de campeon mundial", description = "Lista la seleccion de campeon mundial hecha por jugadores. Rol permitido: ADMIN.")
    public List<PronosticoCampeonMundialResponse> worldChampionPredictions() {
        return championPredictionService.listAll();
    }

    @GetMapping("/predictions")
    @Operation(summary = "Seguimiento de pronosticos", description = "Lista y resume apuestas Colombia, pronosticos globales y campeones mundiales registrados por jugadores. Rol permitido: ADMIN.")
    public AdminPredictionsResponse predictions() {
        return predictionsService.listPredictions();
    }

    @GetMapping("/manual-entry/users")
    @Operation(summary = "Jugadores para registro manual", description = "Lista jugadores disponibles para registrar apuestas o pronosticos manualmente. Rol permitido: ADMIN.")
    public List<UsuarioResponse> manualEntryUsers() {
        return manualEntryService.listPlayers();
    }

    @GetMapping("/manual-entry/matches")
    @Operation(summary = "Partidos para registro manual", description = "Lista partidos guardados en PostgreSQL para correcciones manuales. Rol permitido: ADMIN.")
    public List<PartidoDto> manualEntryMatches() {
        return manualEntryService.listMatches();
    }

    @GetMapping("/manual-entry/users/{userId}/history")
    @Operation(summary = "Historial manual por jugador", description = "Muestra apuestas Colombia y pronosticos globales del jugador con origen y auditoria funcional.")
    public ManualEntryHistoryResponse manualEntryHistory(@PathVariable UUID userId) {
        return manualEntryService.history(userId);
    }

    @PostMapping("/manual-entry/users/{userId}/colombia-bets")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar apuestas Colombia manuales", description = "Permite al ADMIN registrar apuestas Colombia para un jugador sin aplicar cierre por tiempo.")
    public List<ApuestaColombiaResponse> createManualColombiaBets(
            Authentication authentication,
            @PathVariable UUID userId,
            @Valid @RequestBody CrearApuestasColombiaRequest request) {
        return manualEntryService.createColombiaBets(usuarioService.getCurrentUser(authentication), userId, request);
    }

    @PutMapping("/manual-entry/colombia-bets/{betId}")
    @Operation(summary = "Editar apuesta Colombia manualmente", description = "Permite al ADMIN corregir una apuesta Colombia y su principal sin aplicar cierre por tiempo.")
    public ApuestaColombiaResponse updateManualColombiaBet(
            Authentication authentication,
            @PathVariable UUID betId,
            @Valid @RequestBody ActualizarApuestaColombiaRequest request) {
        return manualEntryService.updateColombiaBet(usuarioService.getCurrentUser(authentication), betId, request);
    }

    @DeleteMapping("/manual-entry/colombia-bets/{betId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar apuesta Colombia manualmente", description = "Permite al ADMIN eliminar una apuesta Colombia y recalcular la principal del jugador.")
    public void deleteManualColombiaBet(Authentication authentication, @PathVariable UUID betId) {
        manualEntryService.deleteColombiaBet(usuarioService.getCurrentUser(authentication), betId);
    }

    @PostMapping("/manual-entry/users/{userId}/global-predictions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar pronostico global manual", description = "Permite al ADMIN registrar o actualizar un pronostico global para un jugador sin aplicar cierre por tiempo.")
    public PronosticoGlobalResponse createManualGlobalPrediction(
            Authentication authentication,
            @PathVariable UUID userId,
            @Valid @RequestBody GuardarPronosticoGlobalRequest request) {
        return manualEntryService.createGlobalPrediction(usuarioService.getCurrentUser(authentication), userId, request);
    }

    @PutMapping("/manual-entry/global-predictions/{predictionId}")
    @Operation(summary = "Editar pronostico global manualmente", description = "Permite al ADMIN corregir un pronostico global existente.")
    public PronosticoGlobalResponse updateManualGlobalPrediction(
            Authentication authentication,
            @PathVariable UUID predictionId,
            @Valid @RequestBody ActualizarPronosticoGlobalManualRequest request) {
        return manualEntryService.updateGlobalPrediction(usuarioService.getCurrentUser(authentication), predictionId, request);
    }

    @DeleteMapping("/manual-entry/global-predictions/{predictionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar pronostico global manualmente", description = "Permite al ADMIN eliminar un pronostico global no evaluado.")
    public void deleteManualGlobalPrediction(Authentication authentication, @PathVariable UUID predictionId) {
        manualEntryService.deleteGlobalPrediction(usuarioService.getCurrentUser(authentication), predictionId);
    }

    @PutMapping("/manual-entry/matches/{matchId}/result")
    @Operation(summary = "Guardar resultado manual", description = "Guarda o corrige el resultado 90 minutos de un partido y recalcula puntos/ranking.")
    public ResultadoManualResponse saveManualResult(
            Authentication authentication,
            @PathVariable Long matchId,
            @Valid @RequestBody GuardarResultadoManualRequest request) {
        return manualResultService.saveManualResult(usuarioService.getCurrentUser(authentication), matchId, request);
    }

    @GetMapping("/audit/entries")
    @Operation(summary = "Eventos de auditoria", description = "Lista eventos funcionales seguros con filtros por usuario, accion, entidad y fecha. Rol permitido: ADMIN.")
    public List<AuditEventResponse> auditEntries(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return auditService.list(username, action, entityType, from, to);
    }

    @GetMapping("/audit/export")
    @Operation(summary = "Exportar auditoria a Excel", description = "Descarga un archivo .xlsx con los eventos de auditoria filtrados. Rol permitido: ADMIN.")
    public ResponseEntity<ByteArrayResource> exportAudit(
            Authentication authentication,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        byte[] bytes = auditExportService.toExcel(auditService.list(username, action, entityType, from, to));
        auditService.record(
                usuarioService.getCurrentUser(authentication),
                "AUDIT_EXPORT",
                "AUDIT_EVENT",
                "export",
                "Exporto auditoria a Excel",
                "Excel de auditoria generado");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=famicup-auditoria.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @GetMapping("/evidence/export")
    @Operation(summary = "Exportar evidencia diaria", description = "Genera un Excel operativo con partidos del dia, pronosticos, apuestas Colombia, pagos y campeon mundial.")
    public ResponseEntity<ByteArrayResource> exportEvidence(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "true") boolean includePayments,
            @RequestParam(defaultValue = "true") boolean includeChampion) {
        byte[] bytes = evidenceExportService.exportDailyEvidence(date, includePayments, includeChampion, usuarioService.getCurrentUser(authentication));
        String filename = "famicup-evidencia-" + date + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}
