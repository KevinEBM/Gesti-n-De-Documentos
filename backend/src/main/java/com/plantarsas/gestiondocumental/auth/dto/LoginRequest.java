package com.plantarsas.gestiondocumental.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato valido")
        String correo,

        @NotBlank(message = "La contrasena es obligatoria")
        String contrasena
) {
}
