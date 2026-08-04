package com.plantarsas.gestiondocumental.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String mensaje) {
        this(mensaje, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String mensaje, HttpStatus status) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
