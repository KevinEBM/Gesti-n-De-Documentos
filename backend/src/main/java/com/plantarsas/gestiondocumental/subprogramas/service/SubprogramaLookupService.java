package com.plantarsas.gestiondocumental.subprogramas.service;

import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;

public interface SubprogramaLookupService {

    Subprograma obtenerEntidadPorId(Long id);

    Subprograma obtenerActivoPorId(Long id);
}
