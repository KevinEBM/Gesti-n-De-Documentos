package com.plantarsas.gestiondocumental.areas.dto;

import jakarta.validation.constraints.NotNull;

public record AreaEstadoRequest(
        @NotNull(message = "El estado del área es obligatorio")
        Boolean activo
) {
}
