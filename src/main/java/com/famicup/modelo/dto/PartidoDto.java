package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.EstadoPartido;
import java.time.OffsetDateTime;

public record PartidoDto(
        Long id,
        String stage,
        String groupName,
        String roundName,
        OffsetDateTime kickoffAtUtc,
        EquipoDto homeTeam,
        EquipoDto awayTeam,
        EstadoPartido status,
        String apiStatusShort,
        ResultadoPartidoDto result) {
}
