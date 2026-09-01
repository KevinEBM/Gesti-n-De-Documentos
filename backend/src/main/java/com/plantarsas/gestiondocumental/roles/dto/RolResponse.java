package com.plantarsas.gestiondocumental.roles.dto;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

public record RolResponse(
        Long id,
        RolEnum nombre,
        String descripcion,
        boolean activo
) {
}
