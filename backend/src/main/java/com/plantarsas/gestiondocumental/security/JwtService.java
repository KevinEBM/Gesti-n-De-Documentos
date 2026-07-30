package com.plantarsas.gestiondocumental.security;

public interface JwtService {

    String generarToken(String sujeto);

    String obtenerSujeto(String token);

    boolean esTokenValido(String token);
}

