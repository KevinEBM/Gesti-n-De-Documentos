package com.plantarsas.gestiondocumental.documentos.dto;

import java.io.InputStream;

public record DocumentoArchivoDescarga(
        String nombreArchivoOriginal,
        String tipoMime,
        long tamanoBytes,
        InputStream contenido
) {
}
