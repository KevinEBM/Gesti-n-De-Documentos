package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;

import java.time.Instant;
import java.util.List;

public record DocumentoResponse(
        Long id,
        String codigo,
        String titulo,
        String descripcion,
        DocumentoEstado estado,
        Long areaId,
        String areaNombre,
        Long subprogramaId,
        String subprogramaNombre,
        Long tipoDocumentoId,
        String tipoDocumentoNombre,
        Long creadoPorId,
        int numeroVersionActual,
        String nombreArchivoOriginal,
        String tipoMime,
        long tamanoBytes,
        String descripcionVersionActual,
        Long publicadoPorId,
        Instant fechaPublicacionVersion,
        Instant fechaCreacion,
        Instant fechaActualizacion,
        DocumentoAlcance alcance,
        List<AreaResumenResponse> areasAdicionales
) {
}
