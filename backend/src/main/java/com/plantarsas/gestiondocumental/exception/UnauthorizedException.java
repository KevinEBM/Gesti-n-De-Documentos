package com.plantarsas.gestiondocumental.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String mensaje) {
        super(mensaje, HttpStatus.FORBIDDEN);
    }
}
