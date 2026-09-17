package com.plantarsas.gestiondocumental.areas.dto;

import java.time.LocalDateTime;

/**
 * Datos de un área que se muestran al usuario: su código, nombre,
 * descripción y si está activa.
 */
public record AreaResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
