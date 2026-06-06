package com.famicup.servicio;

import com.famicup.configuracion.ApiFootballProperties;
import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.modelo.dto.PartidoDto;
import com.famicup.modelo.entidad.Equipo;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.repositorio.PartidoRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final PartidoMapper partidoMapper;
    private final BettingParametersService parametersService;
    private final ApiFootballProperties apiFootballProperties;

    public PartidoService(
            PartidoRepository partidoRepository,
            PartidoMapper partidoMapper,
            BettingParametersService parametersService,
            ApiFootballProperties apiFootballProperties) {
        this.partidoRepository = partidoRepository;
        this.partidoMapper = partidoMapper;
        this.parametersService = parametersService;
        this.apiFootballProperties = apiFootballProperties;
    }

    @Cacheable("upcomingMatches")
    @Transactional(readOnly = true)
    public List<PartidoDto> upcomingMatches() {
        return partidoRepository.findUpcomingWithTeams(OffsetDateTime.now(ZoneOffset.UTC)).stream()
                .limit(30)
                .map(partidoMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartidoDto> upcomingColombiaMatches() {
        return partidoRepository.findUpcomingColombiaMatches(colombiaTeamId(), OffsetDateTime.now(ZoneOffset.UTC)).stream()
                .map(partidoMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Partido getRequired(Long matchId) {
        return partidoRepository.findById(matchId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Partido no encontrado."));
    }

    public boolean isColombiaMatch(Partido match) {
        return isColombiaTeam(match.getHomeTeam()) || isColombiaTeam(match.getAwayTeam());
    }

    public boolean isClosedForBetting(Partido match, OffsetDateTime now) {
        return !now.isBefore(match.getKickoffAtUtc().minusMinutes(parametersService.closingMinutesBeforeMatch()));
    }

    private boolean isColombiaTeam(Equipo team) {
        if (team == null) {
            return false;
        }
        Integer colombiaTeamId = colombiaTeamId();
        return "COL".equalsIgnoreCase(team.getFifaCode())
                || "Colombia".equalsIgnoreCase(team.getName())
                || (colombiaTeamId != null && colombiaTeamId.equals(team.getApiFootballId()));
    }

    private Integer colombiaTeamId() {
        return apiFootballProperties.colombia() == null ? null : apiFootballProperties.colombia().teamId();
    }
}
