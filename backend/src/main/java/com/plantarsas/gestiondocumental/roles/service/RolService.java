package com.plantarsas.gestiondocumental.roles.service;

import com.plantarsas.gestiondocumental.roles.dto.RolResponse;

import java.util.List;

/**
 * Contrato para consultar los roles disponibles en el sistema, ya sea
 * el listado completo o uno puntual por su id.
 */
public interface RolService {

    List<RolResponse> listar();

    RolResponse obtenerPorId(Long id);
}
