package com.plantarsas.gestiondocumental.areas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AreaRequest(

        @NotBlank(message = "El código del área es obligatorio")
        @Size(
                max = 20,
                message = "El código del área no puede superar los 20 caracteres"
        )
        String codigo,

        @NotBlank(message = "El nombre del área es obligatorio")
        @Size(
                max = 100,
                message = "El nombre del área no puede superar los 100 caracteres"
        )
        String nombre,

        @Size(
                max = 255,
                message = "La descripción no puede superar los 255 caracteres"
        )
        String descripcion
) {
}
