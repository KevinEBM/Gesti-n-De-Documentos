package com.plantarsas.gestiondocumental.subprogramas.dto;

import jakarta.validation.constraints.NotNull;

public record SubprogramaEstadoRequest(
        @NotNull(message = "El estado del subprograma es obligatorio")
        Boolean activo
) {
}
