package com.plantarsas.gestiondocumental.roles.service;

import com.plantarsas.gestiondocumental.roles.dto.RolResponse;

import java.util.List;

public interface RolService {

    List<RolResponse> listar();

    RolResponse obtenerPorId(Long id);
}
