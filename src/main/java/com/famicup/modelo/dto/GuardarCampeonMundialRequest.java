package com.famicup.modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarCampeonMundialRequest(
        @NotBlank @Size(max = 10) String teamCode) {
}
