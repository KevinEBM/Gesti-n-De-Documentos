package com.plantarsas.gestiondocumental.shared.enums;

/**
 * Criterio de consulta en listados de documentos. No es un estado persistido del documento;
 * solo refina resultados ya autorizados por {@code visiblePara}.
 */
public enum AlcanceConsulta {
    TODOS_VISIBLES,
    MI_AREA,
    GLOBALES,
    AREAS_ESPECIFICAS
}
