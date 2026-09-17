package com.plantarsas.gestiondocumental.subprogramas.service;

import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;

/**
 * Contrato para que otros módulos, como documentos, obtengan un
 * subprograma por su id, con o sin exigir que esté activo.
 */
public interface SubprogramaLookupService {

    Subprograma obtenerEntidadPorId(Long id);

    Subprograma obtenerActivoPorId(Long id);
}
