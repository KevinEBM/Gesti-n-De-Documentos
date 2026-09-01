package com.plantarsas.gestiondocumental.areas.service;

import com.plantarsas.gestiondocumental.areas.dto.AreaEstadoRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;

import java.util.List;

public interface AreaService {

    AreaResponse crear(AreaRequest request);

    List<AreaResponse> listar();

    List<AreaResponse> listarParaUsuario(AuthenticatedUser usuario);

    AreaResponse obtenerPorId(Long id);

    AreaResponse actualizar(Long id, AreaRequest request);

    AreaResponse cambiarEstado(Long id, AreaEstadoRequest request);
}
