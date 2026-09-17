package com.plantarsas.gestiondocumental.tiposdocumento.service;

import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoEstadoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoResponse;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoUpdateRequest;

import java.util.List;

/**
 * Contrato de las operaciones sobre el catálogo de tipos de
 * documento: crear, listar (todos o solo los activos), consultar
 * uno, editarlo y cambiar su estado.
 */
public interface TipoDocumentoService {

    TipoDocumentoResponse crear(TipoDocumentoRequest request);

    List<TipoDocumentoResponse> listar();

    TipoDocumentoResponse obtenerPorId(Long id);

    TipoDocumentoResponse actualizar(Long id, TipoDocumentoUpdateRequest request);

    TipoDocumentoResponse cambiarEstado(Long id, TipoDocumentoEstadoRequest request);

    List<TipoDocumentoResponse> listarActivos();
}
