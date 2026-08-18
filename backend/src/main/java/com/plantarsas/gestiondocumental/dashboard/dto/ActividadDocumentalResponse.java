package com.plantarsas.gestiondocumental.dashboard.dto;

import java.time.LocalDateTime;

public record ActividadDocumentalResponse(
        TipoActividad tipoActividad,
        Long documentoId,
        String codigoDocumento,
        String tituloDocumento,
        int numeroVersion,
        String descripcionCambio,
        LocalDateTime fechaPublicacion,
        Long publicadoPorId,
        String publicadoPorNombres,
        String publicadoPorApellidos
) {
}
