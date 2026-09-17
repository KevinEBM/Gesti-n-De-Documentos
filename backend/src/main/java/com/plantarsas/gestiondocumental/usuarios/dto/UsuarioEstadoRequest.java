package com.plantarsas.gestiondocumental.usuarios.dto;

import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado al que se quiere mover la cuenta de un usuario (activo,
 * inactivo o bloqueado).
 */
public record UsuarioEstadoRequest(
        @NotNull(message = "El estado del usuario es obligatorio")
        EstadoUsuario estado
) {
}
