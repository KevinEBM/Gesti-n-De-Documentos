package com.plantarsas.gestiondocumental.storage;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {

    StoredFile guardar(String nombreOriginal, InputStream contenido, String mimeType, long tamanoBytes) throws IOException;

    InputStream cargar(String ruta) throws IOException;

    void eliminar(String ruta) throws IOException;

    boolean existe(String ruta);
}

