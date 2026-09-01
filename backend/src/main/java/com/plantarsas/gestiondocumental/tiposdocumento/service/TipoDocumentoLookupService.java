package com.plantarsas.gestiondocumental.tiposdocumento.service;

import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;

public interface TipoDocumentoLookupService {

    TipoDocumento obtenerEntidadPorId(Long id);

    TipoDocumento obtenerActivoPorId(Long id);
}
