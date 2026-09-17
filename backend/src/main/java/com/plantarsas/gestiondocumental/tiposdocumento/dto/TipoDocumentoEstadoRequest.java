package com.plantarsas.gestiondocumental.tiposdocumento.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Indica si un tipo de documento debe quedar activo o inactivo.
 */
public record TipoDocumentoEstadoRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
