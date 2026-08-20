package com.plantarsas.gestiondocumental.documentos.service;

import com.plantarsas.gestiondocumental.documentos.dto.DocumentoActualizacionRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoEstadoActualizacionRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoPublicacionInicialRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.NuevaVersionDocumentoRequest;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;

import java.io.InputStream;

public interface DocumentoService {

    DocumentoResponse publicarInicial(
            DocumentoPublicacionInicialRequest request,
            AuthenticatedUser usuarioAutenticado,
            String nombreArchivoOriginal,
            InputStream contenidoArchivo,
            String tipoMimeArchivo,
            long tamanoBytesArchivo
    );

    DocumentoResponse publicarNuevaVersion(
            Long documentoId,
            NuevaVersionDocumentoRequest request,
            AuthenticatedUser usuarioAutenticado,
            String nombreArchivoOriginal,
            InputStream contenidoArchivo,
            String tipoMimeArchivo,
            long tamanoBytesArchivo
    );

    DocumentoResponse actualizarMetadatos(
            Long documentoId,
            DocumentoActualizacionRequest request,
            AuthenticatedUser usuarioAutenticado
    );

    DocumentoResponse cambiarEstado(
            Long documentoId,
            DocumentoEstadoActualizacionRequest request,
            AuthenticatedUser usuarioAutenticado
    );
}
