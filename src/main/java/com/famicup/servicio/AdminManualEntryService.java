package com.famicup.servicio;

import com.famicup.modelo.dto.ActualizarApuestaColombiaRequest;
import com.famicup.modelo.dto.ActualizarPronosticoGlobalManualRequest;
import com.famicup.modelo.dto.ApuestaColombiaResponse;
import com.famicup.modelo.dto.CrearApuestasColombiaRequest;
import com.famicup.modelo.dto.GuardarPronosticoGlobalRequest;
import com.famicup.modelo.dto.ManualEntryHistoryResponse;
import com.famicup.modelo.dto.PartidoDto;
import com.famicup.modelo.dto.PronosticoGlobalResponse;
import com.famicup.modelo.dto.UsuarioResponse;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.modelo.mapper.UsuarioMapper;
import com.famicup.repositorio.ApuestaColombiaRepository;
import com.famicup.repositorio.PartidoRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import com.famicup.repositorio.UsuarioRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManualEntryService {

    private static final String MODALITY_COLOMBIA = "COLOMBIA";
    private static final String MODALITY_GLOBAL = "GLOBAL";

    private final UsuarioRepository usuarioRepository;
    private final PartidoRepository partidoRepository;
    private final ApuestaColombiaRepository colombiaBetRepository;
    private final PronosticoGlobalRepository globalPredictionRepository;
    private final ColombiaBetsService colombiaBetsService;
    private final GlobalPredictionService globalPredictionService;
    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;
    private final PartidoMapper partidoMapper;

    public AdminManualEntryService(
            UsuarioRepository usuarioRepository,
            PartidoRepository partidoRepository,
            ApuestaColombiaRepository colombiaBetRepository,
            PronosticoGlobalRepository globalPredictionRepository,
            ColombiaBetsService colombiaBetsService,
            GlobalPredictionService globalPredictionService,
            UsuarioService usuarioService,
            UsuarioMapper usuarioMapper,
            PartidoMapper partidoMapper) {
        this.usuarioRepository = usuarioRepository;
        this.partidoRepository = partidoRepository;
        this.colombiaBetRepository = colombiaBetRepository;
        this.globalPredictionRepository = globalPredictionRepository;
        this.colombiaBetsService = colombiaBetsService;
        this.globalPredictionService = globalPredictionService;
        this.usuarioService = usuarioService;
        this.usuarioMapper = usuarioMapper;
        this.partidoMapper = partidoMapper;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listPlayers() {
        return usuarioRepository.findByRoleOrderByFullNameAsc(RolUsuario.PLAYER).stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartidoDto> listMatches() {
        return partidoRepository.findAllWithTeamsOrderByKickoffAtUtcAsc().stream()
                .map(partidoMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ManualEntryHistoryResponse history(UUID userId) {
        Usuario player = usuarioService.getById(userId);
        List<ManualEntryHistoryResponse.Item> items = new java.util.ArrayList<>();
        colombiaBetRepository.findByUserOrderByRegisteredAtDesc(player)
                .forEach(bet -> items.add(toColombiaItem(bet)));
        globalPredictionRepository.findByUserOrderByRegisteredAtDesc(player)
                .forEach(prediction -> items.add(toGlobalItem(prediction)));
        items.sort(Comparator
                .comparing(ManualEntryHistoryResponse.Item::updatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());
        return new ManualEntryHistoryResponse(player.getId(), player.getUsername(), player.getFullName(), items);
    }

    @Transactional
    public List<ApuestaColombiaResponse> createColombiaBets(Usuario admin, UUID userId, CrearApuestasColombiaRequest request) {
        return colombiaBetsService.createBetsForAdmin(admin, usuarioService.getById(userId), request);
    }

    @Transactional
    public ApuestaColombiaResponse updateColombiaBet(Usuario admin, UUID betId, ActualizarApuestaColombiaRequest request) {
        return colombiaBetsService.updateBetForAdmin(admin, betId, request);
    }

    @Transactional
    public void deleteColombiaBet(Usuario admin, UUID betId) {
        colombiaBetsService.deleteBetForAdmin(admin, betId);
    }

    @Transactional
    public PronosticoGlobalResponse createGlobalPrediction(Usuario admin, UUID userId, GuardarPronosticoGlobalRequest request) {
        return globalPredictionService.savePredictionForAdmin(admin, usuarioService.getById(userId), request);
    }

    @Transactional
    public PronosticoGlobalResponse updateGlobalPrediction(Usuario admin, UUID predictionId, ActualizarPronosticoGlobalManualRequest request) {
        return globalPredictionService.updatePredictionForAdmin(admin, predictionId, request);
    }

    @Transactional
    public void deleteGlobalPrediction(Usuario admin, UUID predictionId) {
        globalPredictionService.deletePredictionForAdmin(admin, predictionId);
    }

    private ManualEntryHistoryResponse.Item toColombiaItem(ApuestaColombia bet) {
        return new ManualEntryHistoryResponse.Item(
                bet.getId(),
                MODALITY_COLOMBIA,
                partidoMapper.toDto(bet.getMatch()),
                bet.getPredictedHomeGoals(),
                bet.getPredictedAwayGoals(),
                scoreLabel(bet.getPredictedHomeGoals(), bet.getPredictedAwayGoals()),
                bet.isPrincipalGlobalPrediction(),
                bet.getEntryOrigin().name(),
                bet.getCreatedByAdmin() == null ? null : bet.getCreatedByAdmin().getUsername(),
                bet.getUpdatedByAdmin() == null ? null : bet.getUpdatedByAdmin().getUsername(),
                bet.getStatus().name(),
                bet.getRegisteredAt(),
                bet.getUpdatedAt());
    }

    private ManualEntryHistoryResponse.Item toGlobalItem(PronosticoGlobal prediction) {
        return new ManualEntryHistoryResponse.Item(
                prediction.getId(),
                MODALITY_GLOBAL,
                partidoMapper.toDto(prediction.getMatch()),
                prediction.getPredictedHomeGoals(),
                prediction.getPredictedAwayGoals(),
                scoreLabel(prediction.getPredictedHomeGoals(), prediction.getPredictedAwayGoals()),
                false,
                prediction.getEntryOrigin().name(),
                prediction.getCreatedByAdmin() == null ? null : prediction.getCreatedByAdmin().getUsername(),
                prediction.getUpdatedByAdmin() == null ? null : prediction.getUpdatedByAdmin().getUsername(),
                prediction.getStatus().name(),
                prediction.getRegisteredAt(),
                prediction.getUpdatedAt());
    }

    private String scoreLabel(Integer homeGoals, Integer awayGoals) {
        return homeGoals + " - " + awayGoals;
    }
}
