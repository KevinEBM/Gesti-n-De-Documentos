package com.plantarsas.gestiondocumental.exception;

import org.springframework.http.HttpStatus;

/**
 * Se produce cuando un usuario que sí inició sesión correctamente
 * intenta hacer algo que no le corresponde, como consultar un área
 * que no es la suya. Es distinto a un fallo de inicio de sesión: aquí
 * el sistema sabe perfectamente quién es la persona, solo que no
 * tiene permiso para esa acción puntual.
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String mensaje) {
        super(mensaje, HttpStatus.FORBIDDEN);
    }
}
