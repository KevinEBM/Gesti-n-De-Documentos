package com.plantarsas.gestiondocumental.auth.service;

import com.plantarsas.gestiondocumental.auth.dto.LoginRequest;
import com.plantarsas.gestiondocumental.auth.dto.LoginResponse;
import com.plantarsas.gestiondocumental.auth.dto.PerfilUsuarioResponse;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    /** Perfil vigente en base de datos del usuario que porta el token. */
    PerfilUsuarioResponse obtenerPerfilActual(AuthenticatedUser usuarioAutenticado);
}
