package com.plantarsas.gestiondocumental.documentos.dto;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;

import java.time.LocalDate;

public record DocumentoFiltroRequest(
        String codigo,
        String titulo,
        Long areaId,
        Long subprogramaId,
        Long tipoDocumentoId,
        DocumentoEstado estado,
        LocalDate fechaDesde,
        LocalDate fechaHasta
) {
}
