package com.plantarsas.gestiondocumental.exception;

import org.springframework.http.HttpStatus;

/**
 * Se produce cuando un intento de inicio de sesión falla, ya sea
 * porque el correo o la contraseña no son correctos, o porque la
 * cuenta está inactiva. A propósito no dice cuál de las dos cosas
 * pasó, para no darle pistas a alguien que esté intentando adivinar
 * credenciales ajenas.
 */
public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException(String mensaje) {
        super(mensaje, HttpStatus.UNAUTHORIZED);
    }
}
