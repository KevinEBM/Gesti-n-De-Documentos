package com.plantarsas.gestiondocumental.shared.dto;

import java.time.LocalDateTime;
import java.util.Map;

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

