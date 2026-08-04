package com.plantarsas.gestiondocumental.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioUpdateRequest(

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo,

        @NotNull(message = "El rol es obligatorio")
        @Positive(message = "El identificador del rol debe ser positivo")
        Long rolId,

        @NotEmpty(message = "Debe asignarse al menos un área")
        Set<
                @NotNull(message = "Los identificadores de las áreas son obligatorios")
                @Positive(message = "Los identificadores de las áreas deben ser positivos")
                Long
        > areaIds,

        @Positive(message = "El identificador del área principal debe ser positivo")
        Long areaPrincipalId
) {
}
