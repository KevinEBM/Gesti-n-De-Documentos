package com.plantarsas.gestiondocumental.tiposdocumento.service;

import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;

/**
 * Contrato para que otros módulos, como documentos, obtengan un tipo
 * de documento por su id, con o sin exigir que esté activo.
 */
public interface TipoDocumentoLookupService {

    TipoDocumento obtenerEntidadPorId(Long id);

    TipoDocumento obtenerActivoPorId(Long id);
}
