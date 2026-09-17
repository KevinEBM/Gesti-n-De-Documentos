package com.plantarsas.gestiondocumental.storage;

/**
 * Datos de un archivo recién guardado: su nombre original, dónde
 * quedó almacenado, su tipo, su tamaño y el hash calculado para
 * verificar su integridad.
 */
public record StoredFile(
        String nombreOriginal,
        String ruta,
        String mimeType,
        long tamanoBytes,
        String hash
) {
}
