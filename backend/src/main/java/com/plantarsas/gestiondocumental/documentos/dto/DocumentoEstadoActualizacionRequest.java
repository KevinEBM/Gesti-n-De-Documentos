package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import jakarta.validation.constraints.NotNull;

public record DocumentoEstadoActualizacionRequest(
        @NotNull(message = "El estado del documento es obligatorio")
        DocumentoEstado estado
) {
}
