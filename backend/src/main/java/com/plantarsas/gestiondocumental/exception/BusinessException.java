package com.plantarsas.gestiondocumental.exception;

import org.springframework.http.HttpStatus;

/**
 * Error genérico para cuando una operación viola una regla del negocio,
 * como un dato inválido o una acción no permitida en ese momento. Cada
 * error de este tipo lleva indicado con qué código HTTP debe responder
 * la API, así cada regla decide su propia respuesta sin que el resto
 * del código dependa de detalles técnicos internos.
 */
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
