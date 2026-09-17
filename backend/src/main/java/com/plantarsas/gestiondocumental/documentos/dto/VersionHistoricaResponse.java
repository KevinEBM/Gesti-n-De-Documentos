package com.plantarsas.gestiondocumental.documentos.dto;

import java.time.Instant;

/**
 * Datos de una versión anterior de un documento tal como se muestran
 * en su historial: cuándo se publicó, quién la publicó, qué cambió y
 * si sigue siendo la versión vigente.
 */
public record VersionHistoricaResponse(
        Long id,
        int numeroVersion,
        String nombreArchivoOriginal,
        String tipoMime,
        long tamanoBytes,
        String descripcionCambio,
        Instant fechaPublicacion,
        Long publicadoPorId,
        String publicadoPorNombre,
        boolean vigente
) {
}
