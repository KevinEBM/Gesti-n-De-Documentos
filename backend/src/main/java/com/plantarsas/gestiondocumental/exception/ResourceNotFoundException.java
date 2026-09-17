package com.plantarsas.gestiondocumental.exception;

import org.springframework.http.HttpStatus;

/**
 * Se produce cuando se busca algo por su identificador -un documento,
 * un usuario, un área- y no existe, o no está disponible para quien
 * lo pide. Siempre responde con el código 404, el mismo que usan las
 * páginas web para "no encontrado".
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje, HttpStatus.NOT_FOUND);
    }
}
