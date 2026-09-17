package com.plantarsas.gestiondocumental.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que envía un usuario para cambiar su propia contraseña: la
 * contraseña actual (para confirmar que es él), la nueva contraseña y
 * su confirmación.
 */
public record CambiarContrasenaRequest(

        @NotBlank(message = "La contraseña actual es obligatoria")
        String contrasenaActual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(
                min = 8,
                max = 100,
                message = "La contraseña debe tener entre 8 y 100 caracteres"
        )
        String nuevaContrasena,

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        @Size(
                min = 8,
                max = 100,
                message = "La contraseña debe tener entre 8 y 100 caracteres"
        )
        String confirmacionContrasena
) {
}
