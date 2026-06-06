package com.famicup.controlador;

import com.famicup.modelo.dto.ApuestaColombiaResponse;
import com.famicup.modelo.dto.ActualizarApuestaColombiaRequest;
import com.famicup.modelo.dto.CambiarPasswordRequest;
import com.famicup.modelo.dto.CrearApuestasColombiaRequest;
import com.famicup.modelo.dto.EquipoDto;
import com.famicup.modelo.dto.GuardarPronosticoGlobalRequest;
import com.famicup.modelo.dto.GuardarCampeonMundialRequest;
import com.famicup.modelo.dto.HistorialJugadorResponse;
import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.modelo.dto.PartidoDto;
import com.famicup.modelo.dto.PlayerDashboardResponse;
import com.famicup.modelo.dto.PronosticoGlobalResponse;
import com.famicup.modelo.dto.PronosticoCampeonMundialResponse;
import com.famicup.modelo.dto.RankingResponse;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.servicio.BettingParametersService;
import com.famicup.servicio.ColombiaBetsService;
import com.famicup.servicio.DashboardService;
import com.famicup.servicio.GlobalPredictionService;
import com.famicup.servicio.PartidoService;
import com.famicup.servicio.RankingService;
import com.famicup.servicio.UsuarioService;
import com.famicup.servicio.WorldChampionPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
@PreAuthorize("hasRole('PLAYER')")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Player", description = "Operaciones del apostador protegidas para PLAYER.")
public class PlayerController {

    private final UsuarioService usuarioService;
    private final DashboardService dashboardService;
    private final PartidoService partidoService;
    private final ColombiaBetsService colombiaBetsService;
    private final GlobalPredictionService globalPredictionService;
    private final RankingService rankingService;
    private final BettingParametersService parametersService;
    private final WorldChampionPredictionService championPredictionService;

    public PlayerController(
            UsuarioService usuarioService,
            DashboardService dashboardService,
            PartidoService partidoService,
            ColombiaBetsService colombiaBetsService,
            GlobalPredictionService globalPredictionService,
            RankingService rankingService,
            BettingParametersService parametersService,
            WorldChampionPredictionService championPredictionService) {
        this.usuarioService = usuarioService;
        this.dashboardService = dashboardService;
        this.partidoService = partidoService;
        this.colombiaBetsService = colombiaBetsService;
        this.globalPredictionService = globalPredictionService;
        this.rankingService = rankingService;
        this.parametersService = parametersService;
        this.championPredictionService = championPredictionService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard jugador", description = "Resumen personal de posicion, puntos, pagos y pronosticos recientes. Rol permitido: PLAYER.")
    public PlayerDashboardResponse dashboard(Authentication authentication) {
        return dashboardService.playerDashboard(currentUser(authentication));
    }

    @GetMapping("/history")
    @Operation(summary = "Historial jugador", description = "Historial de apuestas Colombia Especial y pronosticos globales del usuario autenticado.")
    public HistorialJugadorResponse history(Authentication authentication) {
        return dashboardService.playerHistory(currentUser(authentication));
    }

    @GetMapping("/ranking")
    @Operation(summary = "Ranking familiar", description = "Ranking global por puntos con desempates: exactos, ganadores y desempeño en partidos de Colombia.")
    public List<RankingResponse> ranking(Authentication authentication) {
        return rankingService.getRanking(currentUser(authentication).getId());
    }

    @GetMapping("/parameters")
    @Operation(summary = "Parametros del reglamento", description = "Obtiene montos, limites, cierre y puntajes vigentes para que el apostador vea reglas reales.")
    public ParametrosApuestasResponse parameters() {
        return parametersService.getParameters();
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Cambiar mi contraseña", description = "Actualiza la contraseña del jugador autenticado, la hashea con BCrypt y desactiva el cambio obligatorio. Rol permitido: PLAYER.")
    public com.famicup.modelo.dto.UsuarioResponse changePassword(
            Authentication authentication,
            @Valid @RequestBody CambiarPasswordRequest request) {
        return usuarioService.changeCurrentUserPassword(currentUser(authentication), request);
    }

    @GetMapping("/colombia-matches")
    @Operation(summary = "Partidos de Colombia", description = "Lista partidos futuros donde juega Colombia para apuestas Colombia Especial.")
    public List<PartidoDto> colombiaMatches() {
        return partidoService.upcomingColombiaMatches();
    }

    @GetMapping("/matches")
    @Operation(summary = "Partidos para pronostico global", description = "Lista partidos futuros guardados en PostgreSQL para registrar pronosticos de la Polla Global. Rol permitido: PLAYER.")
    public List<PartidoDto> matches() {
        return partidoService.upcomingMatches();
    }

    @GetMapping("/teams")
    @Operation(summary = "Equipos disponibles", description = "Lista equipos sincronizados desde PostgreSQL para seleccion de campeon mundial. Rol permitido: PLAYER.")
    public List<EquipoDto> teams() {
        return championPredictionService.availableTeams();
    }

    @PostMapping("/colombia-bets")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar apuestas Colombia", description = "Registra de 1 al maximo configurado de apuestas por partido Colombia. Quedan pendientes hasta confirmar pago.")
    public List<ApuestaColombiaResponse> createColombiaBets(
            Authentication authentication,
            @Valid @RequestBody CrearApuestasColombiaRequest request) {
        return colombiaBetsService.createBets(currentUser(authentication), request);
    }

    @PatchMapping("/colombia-bets/{id}")
    @Operation(summary = "Editar apuesta Colombia", description = "Permite editar una apuesta propia antes del cierre configurado para el partido. Rol permitido: PLAYER.")
    public ApuestaColombiaResponse updateColombiaBet(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarApuestaColombiaRequest request) {
        return colombiaBetsService.updateBet(currentUser(authentication), id, request);
    }

    @DeleteMapping("/colombia-bets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar apuesta Colombia", description = "Elimina una apuesta propia antes del cierre del partido. El jugador puede quedarse sin apuestas para ese partido. Rol permitido: PLAYER.")
    public void deleteColombiaBet(Authentication authentication, @PathVariable UUID id) {
        colombiaBetsService.deleteBet(currentUser(authentication), id);
    }

    @GetMapping("/global-predictions")
    @Operation(summary = "Mis pronosticos globales", description = "Lista pronosticos de Polla Global del jugador autenticado.")
    public List<PronosticoGlobalResponse> globalPredictions(Authentication authentication) {
        return globalPredictionService.history(currentUser(authentication));
    }

    @GetMapping("/world-champion-prediction")
    @Operation(summary = "Mi campeon mundial", description = "Obtiene la seleccion unica de campeon mundial del jugador autenticado.")
    public PronosticoCampeonMundialResponse worldChampionPrediction(Authentication authentication) {
        return championPredictionService.current(currentUser(authentication));
    }

    @PutMapping("/world-champion-prediction")
    @Operation(summary = "Guardar campeon mundial", description = "Crea o actualiza el campeon mundial elegido antes del cierre configurado.")
    public PronosticoCampeonMundialResponse saveWorldChampionPrediction(
            Authentication authentication,
            @Valid @RequestBody GuardarCampeonMundialRequest request) {
        return championPredictionService.save(currentUser(authentication), request);
    }

    @PostMapping("/global-predictions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Guardar pronostico global", description = "Crea o actualiza el unico pronostico del jugador para un partido antes del cierre.")
    public PronosticoGlobalResponse saveGlobalPrediction(
            Authentication authentication,
            @Valid @RequestBody GuardarPronosticoGlobalRequest request) {
        return globalPredictionService.savePrediction(currentUser(authentication), request);
    }

    private Usuario currentUser(Authentication authentication) {
        return usuarioService.getCurrentUser(authentication);
    }
}
