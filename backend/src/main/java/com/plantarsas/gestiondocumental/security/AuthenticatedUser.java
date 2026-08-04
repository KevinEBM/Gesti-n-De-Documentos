package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

public record AuthenticatedUser(
        Long id,
        String correo,
        RolEnum rol
) {
}
