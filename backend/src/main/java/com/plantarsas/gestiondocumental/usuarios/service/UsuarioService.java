package com.plantarsas.gestiondocumental.usuarios.service;

import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioEstadoRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioResponse;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioUpdateRequest;

import java.util.List;

public interface UsuarioService {

    UsuarioResponse crear(UsuarioRequest request);

    List<UsuarioResponse> listar();

    UsuarioResponse obtenerPorId(Long id);

    UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request);

    UsuarioResponse cambiarEstado(Long id, UsuarioEstadoRequest request);
}
