package com.famicup.modelo.dto;

public record EquipoDto(
        String fifaCode,
        Integer apiFootballId,
        String name,
        String country,
        String flagUrl) {
}
