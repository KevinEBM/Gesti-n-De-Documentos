package com.plantarsas.gestiondocumental.roles.dto;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

/**
 * Datos de un rol que se le muestran al usuario: su nombre, una
 * descripción y si está activo.
 */
public record RolResponse(
        Long id,
        RolEnum nombre,
        String descripcion,
        boolean activo
) {
}
