package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;

import java.time.Instant;
import java.util.List;

/**
 * Toda la información de un documento que se le muestra al usuario en
 * el detalle: sus datos generales, el área, subprograma y tipo a los
 * que pertenece, los datos de su versión actual, y si ya está en
 * condiciones de eliminarse por haber cumplido el tiempo de retención.
 */
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
        Instant fechaObsolescencia,
        Instant fechaDisponibleEliminacion,
        boolean aptoParaEliminacion,
        DocumentoAlcance alcance,
        List<AreaResumenResponse> areasAdicionales
) {
}
