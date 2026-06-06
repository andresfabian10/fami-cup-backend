package com.famicup.modelo.dto;

import com.famicup.modelo.enumeracion.GanadorPartido;

public record ResultadoPartidoDto(
        int homeGoals90,
        int awayGoals90,
        GanadorPartido winner90) {
}
