package com.plantarsas.gestiondocumental.auth.service;

import com.plantarsas.gestiondocumental.auth.dto.LoginRequest;
import com.plantarsas.gestiondocumental.auth.dto.LoginResponse;
import com.plantarsas.gestiondocumental.auth.dto.PerfilUsuarioResponse;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;

/**
 * Contrato de las operaciones de autenticación: iniciar sesión y
 * consultar el perfil vigente del usuario que ya inició sesión.
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    /** Perfil vigente en base de datos del usuario que porta el token. */
    PerfilUsuarioResponse obtenerPerfilActual(AuthenticatedUser usuarioAutenticado);
}
