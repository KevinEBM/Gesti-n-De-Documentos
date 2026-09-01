package com.plantarsas.gestiondocumental.documentos.dto;

import java.time.Instant;

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
