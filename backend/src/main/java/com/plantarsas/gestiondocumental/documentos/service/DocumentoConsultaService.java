package com.plantarsas.gestiondocumental.documentos.service;

import com.plantarsas.gestiondocumental.documentos.dto.DocumentoArchivoDescarga;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoFiltroRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentoConsultaService {

    Page<DocumentoResumenResponse> listar(
            AuthenticatedUser usuarioAutenticado, DocumentoFiltroRequest filtro, Pageable pageable
    );

    DocumentoResponse obtenerPorId(Long documentoId, AuthenticatedUser usuarioAutenticado);

    DocumentoArchivoDescarga descargarVersionVigente(Long documentoId, AuthenticatedUser usuarioAutenticado);
}
