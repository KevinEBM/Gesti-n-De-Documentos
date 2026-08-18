package com.plantarsas.gestiondocumental.documentos.dto;

import java.time.LocalDateTime;

public record VersionHistoricaResponse(
        Long id,
        int numeroVersion,
        String nombreArchivoOriginal,
        String tipoMime,
        long tamanoBytes,
        String descripcionCambio,
        LocalDateTime fechaPublicacion,
        Long publicadoPorId,
        boolean vigente
) {
}
