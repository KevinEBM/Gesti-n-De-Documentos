package com.plantarsas.gestiondocumental.auth.dto;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

public record LoginResponse(
        String token,
        String tipo,
        Long id,
        String correo,
        String nombres,
        String apellidos,
        RolEnum rol
) {
}
