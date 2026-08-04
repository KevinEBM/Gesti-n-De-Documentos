package com.plantarsas.gestiondocumental.areas.dto;

import java.time.LocalDateTime;

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
