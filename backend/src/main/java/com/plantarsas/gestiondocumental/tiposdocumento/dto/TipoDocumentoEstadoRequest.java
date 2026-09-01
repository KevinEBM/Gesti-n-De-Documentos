package com.plantarsas.gestiondocumental.tiposdocumento.dto;

import jakarta.validation.constraints.NotNull;

public record TipoDocumentoEstadoRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
