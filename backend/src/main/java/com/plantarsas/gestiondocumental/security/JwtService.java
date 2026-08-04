package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;

public interface JwtService {

    String generarToken(Long id, String correo, RolEnum rol);

    Long obtenerIdUsuario(String token);

    String obtenerCorreo(String token);

    RolEnum obtenerRol(String token);

    boolean esTokenValido(String token);
}
