package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

/**
 * Representa a la persona que hizo la petición, una vez que el sistema
 * ya comprobó que su sesión es válida. Es lo que queda disponible para
 * el resto del backend en cada solicitud: quién es (su id y correo) y
 * con qué rol está actuando.
 */
public record AuthenticatedUser(
        Long id,
        String correo,
        RolEnum rol
) {
}
