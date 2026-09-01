package com.plantarsas.gestiondocumental.auth.dto;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

/**
 * Estado actual en base de datos del usuario autenticado. Es la fuente autoritativa del
 * perfil: el snapshot guardado al iniciar sesión puede quedar obsoleto si un administrador
 * reasigna el área o cambia el rol.
 */
public record PerfilUsuarioResponse(
        Long id,
        String correo,
        String nombres,
        String apellidos,
        RolEnum rol,
        Long areaPrincipalId,
        String areaPrincipalNombre
) {
}
