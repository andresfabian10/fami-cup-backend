package com.famicup.modelo.mapper;

import com.famicup.modelo.dto.EquipoDto;
import com.famicup.modelo.dto.PartidoDto;
import com.famicup.modelo.dto.ResultadoPartidoDto;
import com.famicup.modelo.entidad.Equipo;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.ResultadoPartido;
import com.famicup.repositorio.ResultadoPartidoRepository;
import org.springframework.stereotype.Component;

@Component
public class PartidoMapper {

    private final ResultadoPartidoRepository resultadoPartidoRepository;

    public PartidoMapper(ResultadoPartidoRepository resultadoPartidoRepository) {
        this.resultadoPartidoRepository = resultadoPartidoRepository;
    }

    public EquipoDto toTeamDto(Equipo team) {
        if (team == null) {
            return null;
        }
        return new EquipoDto(team.getFifaCode(), team.getApiFootballId(), team.getName(), team.getCountry(), team.getFlagUrl());
    }

    public PartidoDto toDto(Partido match) {
        ResultadoPartidoDto result = resultadoPartidoRepository.findByMatchId(match.getId())
                .map(this::toResultDto)
                .orElse(null);
        return new PartidoDto(
                match.getId(),
                match.getStage(),
                match.getGroupName(),
                match.getRoundName(),
                match.getKickoffAtUtc(),
                toTeamDto(match.getHomeTeam()),
                toTeamDto(match.getAwayTeam()),
                match.getStatus(),
                match.getApiStatusShort(),
                result);
    }

    public ResultadoPartidoDto toResultDto(ResultadoPartido result) {
        return new ResultadoPartidoDto(result.getHomeGoals90(), result.getAwayGoals90(), result.getWinner90());
    }
}
