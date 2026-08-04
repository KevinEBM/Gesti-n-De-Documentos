package com.plantarsas.gestiondocumental.usuarios.dto;

import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record UsuarioEstadoRequest(
        @NotNull(message = "El estado del usuario es obligatorio")
        EstadoUsuario estado
) {
}
