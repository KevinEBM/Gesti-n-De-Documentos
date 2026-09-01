package com.plantarsas.gestiondocumental.subprogramas.dto;

import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;

import java.time.LocalDateTime;

public record SubprogramaResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        AreaResponse area,
        boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
