package com.plantarsas.gestiondocumental.shared.enums;

/**
 * Indica en qué momento de su vida está un documento: publicado y
 * vigente, inactivo temporalmente, u obsoleto. También sabe qué
 * cambios de estado están permitidos (un documento obsoleto solo
 * puede volver a publicarse) y qué se puede hacer con el documento
 * en cada estado, como editarlo o subir una nueva versión.
 */
public enum DocumentoEstado {
    PUBLICADO,
    INACTIVO,
    OBSOLETO;

    public boolean permiteTransicionA(DocumentoEstado destino) {
        if (destino == null) {
            return false;
        }
        if (this == destino) {
            return true;
        }
        return switch (this) {
            case PUBLICADO -> destino == INACTIVO || destino == OBSOLETO;
            case INACTIVO -> destino == PUBLICADO || destino == OBSOLETO;
            case OBSOLETO -> destino == PUBLICADO;
        };
    }

    public boolean permitePublicarNuevaVersion() {
        return permiteEditarPublicacion();
    }

    public boolean permiteEditarPublicacion() {
        return this != OBSOLETO;
    }
}

