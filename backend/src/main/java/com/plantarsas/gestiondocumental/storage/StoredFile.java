package com.plantarsas.gestiondocumental.storage;

public record StoredFile(
        String nombreOriginal,
        String ruta,
        String mimeType,
        long tamanoBytes,
        String hash
) {
}
