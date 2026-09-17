package com.plantarsas.gestiondocumental.areas.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Indica si un área debe quedar activa o inactiva.
 */
public record AreaEstadoRequest(
        @NotNull(message = "El estado del área es obligatorio")
        Boolean activo
) {
}
