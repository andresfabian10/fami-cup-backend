package com.famicup.servicio;

import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.dto.EquipoDto;
import com.famicup.modelo.dto.GuardarCampeonMundialRequest;
import com.famicup.modelo.dto.PronosticoCampeonMundialResponse;
import com.famicup.modelo.entidad.Equipo;
import com.famicup.modelo.entidad.PronosticoCampeonMundial;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoCampeonMundial;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.repositorio.EquipoRepository;
import com.famicup.repositorio.PronosticoCampeonMundialRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorldChampionPredictionService {

    private final PronosticoCampeonMundialRepository predictionRepository;
    private final EquipoRepository equipoRepository;
    private final BettingParametersService parametersService;
    private final PartidoMapper partidoMapper;
    private final AuditService auditService;

    public WorldChampionPredictionService(
            PronosticoCampeonMundialRepository predictionRepository,
            EquipoRepository equipoRepository,
            BettingParametersService parametersService,
            PartidoMapper partidoMapper,
            AuditService auditService) {
        this.predictionRepository = predictionRepository;
        this.equipoRepository = equipoRepository;
        this.parametersService = parametersService;
        this.partidoMapper = partidoMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EquipoDto> availableTeams() {
        return equipoRepository.findAllByOrderByNameAsc().stream()
                .filter(team -> team.getFifaCode() != null && !team.getFifaCode().isBlank())
                .map(partidoMapper::toTeamDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PronosticoCampeonMundialResponse current(Usuario user) {
        return predictionRepository.findByUser(user)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PronosticoCampeonMundialResponse> listAll() {
        return predictionRepository.findAllByOrderByUpdatedAtDesc().stream()
                .sorted(Comparator.comparing(prediction -> prediction.getUser().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PronosticoCampeonMundialResponse save(Usuario user, GuardarCampeonMundialRequest request) {
        if (isLocked()) {
            throw new ReglaNegocioException("La eleccion de campeon mundial ya esta cerrada.");
        }

        String teamCode = request.teamCode().trim().toUpperCase();
        Equipo team = equipoRepository.findById(teamCode)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado para campeon mundial."));

        PronosticoCampeonMundial prediction = predictionRepository.findByUser(user)
                .orElseGet(PronosticoCampeonMundial::new);
        prediction.setUser(user);
        prediction.setTeam(team);
        prediction.setStatus(EstadoCampeonMundial.VALID);
        prediction.setPoints(0);
        PronosticoCampeonMundial saved = predictionRepository.save(prediction);

        auditService.record(
                user,
                "WORLD_CHAMPION_SAVE",
                "WORLD_CHAMPION_PREDICTION",
                saved.getId().toString(),
                "Selecciono campeon: " + team.getName(),
                "Pronostico de campeon guardado");
        return toResponse(saved);
    }

    private PronosticoCampeonMundialResponse toResponse(PronosticoCampeonMundial prediction) {
        return new PronosticoCampeonMundialResponse(
                prediction.getId(),
                prediction.getUser().getId(),
                prediction.getUser().getUsername(),
                prediction.getUser().getFullName(),
                partidoMapper.toTeamDto(prediction.getTeam()),
                isLocked() && prediction.getStatus() == EstadoCampeonMundial.VALID ? EstadoCampeonMundial.LOCKED : prediction.getStatus(),
                prediction.getPoints(),
                parametersService.worldChampionPoints(),
                !isLocked(),
                parametersService.worldChampionLockAt(),
                prediction.getCreatedAt(),
                prediction.getUpdatedAt());
    }

    private boolean isLocked() {
        OffsetDateTime lockAt = parametersService.worldChampionLockAt();
        return lockAt != null && !OffsetDateTime.now(ZoneOffset.UTC).isBefore(lockAt);
    }
}
