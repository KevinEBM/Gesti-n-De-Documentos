package com.plantarsas.gestiondocumental.exception;

public class ContrasenaActualIncorrectaException extends BusinessException {

    public ContrasenaActualIncorrectaException() {
        super("La contraseña actual es incorrecta.");
    }
}
