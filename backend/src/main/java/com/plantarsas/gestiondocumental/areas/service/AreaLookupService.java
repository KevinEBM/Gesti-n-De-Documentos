package com.plantarsas.gestiondocumental.areas.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;

import java.util.List;

/**
 * Contrato para que otros módulos, como usuarios y documentos,
 * obtengan una o varias áreas por su id, ya sea sin importar su
 * estado o exigiendo que estén activas.
 */
public interface AreaLookupService {

    Area obtenerEntidadPorId(Long id);

    Area obtenerActivaPorId(Long id);

    List<Area> obtenerActivasPorIds(List<Long> ids);
}
