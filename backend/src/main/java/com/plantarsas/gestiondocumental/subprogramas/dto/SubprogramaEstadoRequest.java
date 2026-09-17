package com.plantarsas.gestiondocumental.subprogramas.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Indica si un subprograma debe quedar activo o inactivo.
 */
public record SubprogramaEstadoRequest(
        @NotNull(message = "El estado del subprograma es obligatorio")
        Boolean activo
) {
}
