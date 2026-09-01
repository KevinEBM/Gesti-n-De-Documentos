package com.plantarsas.gestiondocumental.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje, HttpStatus.NOT_FOUND);
    }
}
