package com.plantarsas.gestiondocumental.areas.service;

import com.plantarsas.gestiondocumental.areas.dto.AreaEstadoRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;

import java.util.List;

public interface AreaService {

    AreaResponse crear(AreaRequest request);

    List<AreaResponse> listar();

    AreaResponse obtenerPorId(Long id);

    AreaResponse actualizar(Long id, AreaRequest request);

    AreaResponse cambiarEstado(Long id, AreaEstadoRequest request);
}
