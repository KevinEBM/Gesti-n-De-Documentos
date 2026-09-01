package com.plantarsas.gestiondocumental.subprogramas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubprogramaUpdateRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede superar los 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion,

        @NotNull(message = "El área es obligatoria")
        @Positive(message = "El identificador del área debe ser positivo")
        Long areaId
) {
}
