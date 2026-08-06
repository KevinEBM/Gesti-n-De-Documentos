package com.plantarsas.gestiondocumental.documentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NuevaVersionDocumentoRequest(

        @NotBlank(message = "La descripción del cambio es obligatoria")
        @Size(max = 500, message = "La descripción del cambio no puede superar 500 caracteres")
        String descripcionCambio
) {
}
