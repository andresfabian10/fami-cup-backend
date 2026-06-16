package com.famicup.servicio;

import com.famicup.modelo.dto.AdminDashboardResponse;
import com.famicup.modelo.dto.HistorialJugadorResponse;
import com.famicup.modelo.dto.PlayerDashboardResponse;
import com.famicup.modelo.dto.RankingResponse;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.enumeracion.EstadoUsuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.modelo.enumeracion.SistemaPago;
import com.famicup.repositorio.PagoRepository;
import com.famicup.repositorio.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final PartidoService partidoService;
    private final RankingService rankingService;
    private final PagoService pagoService;
    private final ColombiaBetsService colombiaBetsService;
    private final GlobalPredictionService globalPredictionService;

    public DashboardService(
            UsuarioRepository usuarioRepository,
            PagoRepository pagoRepository,
            PartidoService partidoService,
            RankingService rankingService,
            PagoService pagoService,
            ColombiaBetsService colombiaBetsService,
            GlobalPredictionService globalPredictionService) {
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.partidoService = partidoService;
        this.rankingService = rankingService;
        this.pagoService = pagoService;
        this.colombiaBetsService = colombiaBetsService;
        this.globalPredictionService = globalPredictionService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse adminDashboard() {
        List<RankingResponse> ranking = rankingService.getRanking(null).stream().limit(5).toList();
        return new AdminDashboardResponse(
                usuarioRepository.countByRole(RolUsuario.PLAYER),
                usuarioRepository.countByRoleAndStatus(RolUsuario.PLAYER, EstadoUsuario.ACTIVE),
                pagoRepository.countByStatus(EstadoPago.PAID),
                pagoRepository.countByStatus(EstadoPago.PENDING),
                pagoRepository.totalPaid(),
                pagoRepository.totalPaidBySystem(SistemaPago.GLOBAL),
                pagoRepository.totalPaidBySystem(SistemaPago.COLOMBIA),
                partidoService.upcomingMatches().stream().limit(5).toList(),
                ranking);
    }

    @Transactional
    public PlayerDashboardResponse playerDashboard(Usuario user) {
        var activePredictions = globalPredictionService.history(user).stream().limit(5).toList();
        List<RankingResponse> ranking = rankingService.getRanking(user.getId());
        int position = ranking.stream()
                .filter(row -> row.userId().equals(user.getId()))
                .map(RankingResponse::position)
                .findFirst()
                .orElse(0);
        int points = ranking.stream()
                .filter(row -> row.userId().equals(user.getId()))
                .map(RankingResponse::points)
                .findFirst()
                .orElse(0);
        EstadoPago globalPaymentStatus = pagoRepository.findFirstByUserAndSystemOrderByCreatedAtDesc(user, SistemaPago.GLOBAL)
                .map(payment -> payment.getStatus())
                .orElse(EstadoPago.PENDING);
        return new PlayerDashboardResponse(
                position,
                ranking.size(),
                points,
                globalPaymentStatus,
                activePredictions,
                pagoService.listPaymentsByUser(user));
    }

    @Transactional
    public HistorialJugadorResponse playerHistory(Usuario user) {
        return new HistorialJugadorResponse(
                colombiaBetsService.history(user),
                globalPredictionService.history(user));
    }
}
