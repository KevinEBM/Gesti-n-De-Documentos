package com.plantarsas.gestiondocumental.usuarios.service;

import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioEstadoRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioResponse;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioUpdateRequest;

import java.util.List;

/**
 * Contrato de las operaciones sobre usuarios: crear, listar, consultar
 * uno, editarlo, cambiar su estado y cambiar su contraseña.
 */
public interface UsuarioService {

    UsuarioResponse crear(UsuarioRequest request);

    List<UsuarioResponse> listar();

    UsuarioResponse obtenerPorId(Long id);

    UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request);

    UsuarioResponse cambiarEstado(Long id, UsuarioEstadoRequest request);

    void cambiarContrasena(Long usuarioId, String contrasenaActual, String nuevaContrasena);
}
