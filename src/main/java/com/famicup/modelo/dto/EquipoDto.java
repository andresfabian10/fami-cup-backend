package com.famicup.modelo.dto;

public record EquipoDto(
        String fifaCode,
        Integer apiFootballId,
        String name,
        String displayName,
        String country,
        String flagUrl) {
}
