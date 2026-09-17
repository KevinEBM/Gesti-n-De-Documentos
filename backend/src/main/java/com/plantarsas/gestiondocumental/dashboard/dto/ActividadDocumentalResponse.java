package com.plantarsas.gestiondocumental.dashboard.dto;

import java.time.Instant;

/**
 * Una entrada de la actividad reciente del sistema: una publicación
 * nueva o una nueva versión de un documento, con quién la hizo y
 * cuándo.
 */
public record ActividadDocumentalResponse(
        TipoActividad tipoActividad,
        Long documentoId,
        String codigoDocumento,
        String tituloDocumento,
        int numeroVersion,
        String descripcionCambio,
        Instant fechaPublicacion,
        Long publicadoPorId,
        String publicadoPorNombres,
        String publicadoPorApellidos
) {
}
