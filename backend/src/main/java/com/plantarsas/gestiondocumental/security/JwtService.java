package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

/**
 * Contrato para generar el token de sesión que recibe un usuario al
 * iniciar sesión, y para comprobar más adelante si un token que llega
 * en una petición sigue siendo válido.
 */
public interface JwtService {

    String generarToken(Long id, String correo, RolEnum rol);

    Long obtenerIdUsuario(String token);

    boolean esTokenValido(String token);
}
