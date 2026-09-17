package com.plantarsas.gestiondocumental.shared.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Formato en el que el backend entrega un listado dividido en páginas
 * -por ejemplo, la lista de documentos o de usuarios- para que el
 * frontend sepa cuántos resultados hay en total y en qué página está
 * el usuario. Se arma a partir del resultado interno de la consulta a
 * la base de datos, sin exponer ese detalle técnico hacia afuera.
 */
public record PageResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {

    public static <T> PageResponse<T> desde(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
