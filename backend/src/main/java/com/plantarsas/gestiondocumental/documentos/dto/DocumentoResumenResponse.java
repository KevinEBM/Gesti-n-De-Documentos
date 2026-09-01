package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;

import java.time.Instant;

public record DocumentoResumenResponse(
        Long id,
        String codigo,
        String titulo,
        DocumentoEstado estado,
        DocumentoAlcance alcance,
        String subprogramaNombre,
        String tipoDocumentoNombre,
        Instant fechaActualizacion
) {
}
