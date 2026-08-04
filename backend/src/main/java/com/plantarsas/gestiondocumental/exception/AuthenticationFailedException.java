package com.plantarsas.gestiondocumental.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException(String mensaje) {
        super(mensaje, HttpStatus.UNAUTHORIZED);
    }
}
