package com.plantarsas.gestiondocumental.shared.enums;

/**
 * Define quién puede ver un documento según su área, decidido por el
 * administrador cuando lo publica: toda la organización, solo el área
 * responsable, o un grupo específico de áreas. A diferencia de
 * AlcanceConsulta (que solo filtra una búsqueda), este valor queda
 * guardado como parte del documento.
 */
public enum DocumentoAlcance {
    AREA_RESPONSABLE,
    AREAS_ESPECIFICAS,
    GLOBAL
}
