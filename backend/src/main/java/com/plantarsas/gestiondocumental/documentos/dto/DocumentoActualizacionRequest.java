package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DocumentoActualizacionRequest(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50, message = "El código no puede superar 50 caracteres")
        String codigo,

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede superar 200 caracteres")
        String titulo,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El área es obligatoria")
        @Positive(message = "El área debe ser un identificador válido")
        Long areaId,

        @NotNull(message = "El subprograma es obligatorio")
        @Positive(message = "El subprograma debe ser un identificador válido")
        Long subprogramaId,

        @NotNull(message = "El tipo de documento es obligatorio")
        @Positive(message = "El tipo de documento debe ser un identificador válido")
        Long tipoDocumentoId,

        @NotNull(message = "El alcance es obligatorio")
        DocumentoAlcance alcance,

        @NotNull(message = "La lista de áreas adicionales es obligatoria")
        List<@NotNull(message = "El identificador de área adicional no puede ser nulo")
             @Positive(message = "El identificador de área adicional debe ser un identificador válido")
             Long> areasAdicionalesIds
) {
}
