package com.plantarsas.gestiondocumental.documentos.dto;

import java.io.InputStream;

/**
 * Contiene lo necesario para entregarle al usuario el archivo de una
 * versión de un documento: su nombre original, el tipo de archivo, el
 * tamaño y el contenido que se va a descargar.
 */
public record DocumentoArchivoDescarga(
        String nombreArchivoOriginal,
        String tipoMime,
        long tamanoBytes,
        InputStream contenido
) {
}
