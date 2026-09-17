package com.plantarsas.gestiondocumental.tiposdocumento.dto;

import java.time.LocalDateTime;

/**
 * Datos de un tipo de documento que se muestran al usuario: su
 * código, nombre, descripción y si está activo.
 */
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
