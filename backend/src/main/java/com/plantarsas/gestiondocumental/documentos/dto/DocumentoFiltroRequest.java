package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.AlcanceConsulta;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;

import java.time.LocalDate;

/**
 * Agrupa todos los filtros opcionales con los que se puede refinar
 * una búsqueda de documentos: código, título, área, subprograma,
 * tipo, estado, rango de fechas y alcance de la consulta.
 */
public record DocumentoFiltroRequest(
        String codigo,
        String titulo,
        Long areaId,
        Long subprogramaId,
        Long tipoDocumentoId,
        DocumentoEstado estado,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        AlcanceConsulta alcanceConsulta
) {
}
