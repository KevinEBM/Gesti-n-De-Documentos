package com.plantarsas.gestiondocumental.areas.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;

import java.util.List;

public interface AreaLookupService {

    Area obtenerEntidadPorId(Long id);

    Area obtenerActivaPorId(Long id);

    List<Area> obtenerActivasPorIds(List<Long> ids);
}
