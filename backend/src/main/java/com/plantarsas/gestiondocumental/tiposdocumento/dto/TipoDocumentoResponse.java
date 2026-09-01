package com.plantarsas.gestiondocumental.tiposdocumento.dto;

import java.time.LocalDateTime;

public record TipoDocumentoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
