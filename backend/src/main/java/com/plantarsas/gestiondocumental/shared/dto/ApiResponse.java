package com.plantarsas.gestiondocumental.shared.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único en el que el backend responde siempre, sea la operación
 * exitosa o fallida, para que el frontend no tenga que interpretar una
 * forma distinta de respuesta en cada pantalla. Incluye si hubo éxito,
 * un mensaje, los datos cuando corresponde, y qué campos fallaron si
 * el error fue de validación.
 */
public record ApiResponse<T>(
        boolean exito,
        String mensaje,
        T datos,
        Map<String, String> errores,
        LocalDateTime fechaHora
) {

    public static <T> ApiResponse<T> exitosa(T datos) {
        return new ApiResponse<>(true, null, datos, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String mensaje) {
        return new ApiResponse<>(false, mensaje, null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> errorValidacion(Map<String, String> errores) {
        return new ApiResponse<>(false, "Error de validación", null, errores, LocalDateTime.now());
    }
}

