package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado al que se quiere mover un documento (publicado,
 * inactivo u obsoleto), enviado por el administrador al cambiar su
 * estado.
 */
public record DocumentoEstadoActualizacionRequest(
        @NotNull(message = "El estado del documento es obligatorio")
        DocumentoEstado estado
) {
}
