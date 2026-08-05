package com.plantarsas.gestiondocumental.tiposdocumento.service;

import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoEstadoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoResponse;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoUpdateRequest;

import java.util.List;

public interface TipoDocumentoService {

    TipoDocumentoResponse crear(TipoDocumentoRequest request);

    List<TipoDocumentoResponse> listar();

    TipoDocumentoResponse obtenerPorId(Long id);

    TipoDocumentoResponse actualizar(Long id, TipoDocumentoUpdateRequest request);

    TipoDocumentoResponse cambiarEstado(Long id, TipoDocumentoEstadoRequest request);

    List<TipoDocumentoResponse> listarActivos();
}
