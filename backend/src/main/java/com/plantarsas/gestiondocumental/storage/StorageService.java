package com.plantarsas.gestiondocumental.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Contrato para guardar, leer, eliminar y comprobar la existencia de
 * los archivos de los documentos, sin que el resto del sistema
 * necesite saber dónde ni cómo se guardan físicamente.
 */
public interface StorageService {

    StoredFile guardar(String nombreOriginal, InputStream contenido, String mimeType, long tamanoBytes) throws IOException;

    InputStream cargar(String ruta) throws IOException;

    void eliminar(String ruta) throws IOException;

    boolean existe(String ruta);
}

