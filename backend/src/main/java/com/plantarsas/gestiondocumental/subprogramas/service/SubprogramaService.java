package com.plantarsas.gestiondocumental.subprogramas.service;

import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaEstadoRequest;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaRequest;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaResponse;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaUpdateRequest;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;

import java.util.List;

public interface SubprogramaService {

    SubprogramaResponse crear(SubprogramaRequest request);

    List<SubprogramaResponse> listar();

    List<SubprogramaResponse> listarParaUsuario(AuthenticatedUser usuario);

    SubprogramaResponse obtenerPorId(Long id);

    SubprogramaResponse actualizar(Long id, SubprogramaUpdateRequest request);

    SubprogramaResponse cambiarEstado(Long id, SubprogramaEstadoRequest request);

    List<SubprogramaResponse> listarActivosPorArea(Long areaId, AuthenticatedUser usuario);
}
