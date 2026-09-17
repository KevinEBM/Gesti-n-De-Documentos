package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;

import java.time.Instant;

/**
 * Versión resumida de un documento, pensada para listados: solo los
 * datos que se muestran en una fila de la tabla, sin el detalle
 * completo ni los datos de la versión.
 */
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
