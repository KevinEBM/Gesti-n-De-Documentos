package com.plantarsas.gestiondocumental.documentos.dto;

/**
 * Versión resumida de un área (solo su id y nombre), usada dentro de
 * la respuesta de un documento para listar sus áreas adicionales sin
 * traer toda la información del área.
 */
public record AreaResumenResponse(
        Long id,
        String nombre
) {
}
