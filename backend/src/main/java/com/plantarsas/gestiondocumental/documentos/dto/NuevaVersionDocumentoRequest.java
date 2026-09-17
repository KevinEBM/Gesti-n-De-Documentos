package com.plantarsas.gestiondocumental.documentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que envía el administrador al subir una nueva versión de un
 * documento: solo la descripción de qué cambió, ya que el número de
 * versión lo calcula el sistema automáticamente.
 */
public record NuevaVersionDocumentoRequest(

        @NotBlank(message = "La descripción del cambio es obligatoria")
        @Size(max = 500, message = "La descripción del cambio no puede superar 500 caracteres")
        String descripcionCambio
) {
}
