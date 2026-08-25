package com.plantarsas.gestiondocumental.dashboard.dto;

import java.time.Instant;

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
