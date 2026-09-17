package com.plantarsas.gestiondocumental.auth.dto;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

/**
 * Lo que recibe el usuario al iniciar sesión correctamente: el token
 * de sesión, sus datos básicos, su rol y su área principal.
 */
public record LoginResponse(
        String token,
        String tipo,
        Long id,
        String correo,
        String nombres,
        String apellidos,
        RolEnum rol,
        Long areaPrincipalId,
        String areaPrincipalNombre
) {
}
