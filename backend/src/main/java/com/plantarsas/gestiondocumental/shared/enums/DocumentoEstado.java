package com.plantarsas.gestiondocumental.shared.enums;

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
        return this != OBSOLETO;
    }
}

