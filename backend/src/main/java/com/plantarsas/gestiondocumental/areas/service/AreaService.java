package com.plantarsas.gestiondocumental.areas.service;

import com.plantarsas.gestiondocumental.areas.dto.AreaEstadoRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;

import java.util.List;

/**
 * Contrato de las operaciones sobre el catálogo de áreas: crear,
 * listar (todas o solo las que puede ver un usuario según su rol),
 * consultar una, editarla y cambiar su estado.
 */
public interface AreaService {

    AreaResponse crear(AreaRequest request);

    List<AreaResponse> listar();

    List<AreaResponse> listarParaUsuario(AuthenticatedUser usuario);

    AreaResponse obtenerPorId(Long id);

    AreaResponse actualizar(Long id, AreaRequest request);

    AreaResponse cambiarEstado(Long id, AreaEstadoRequest request);
}
