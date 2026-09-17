package com.plantarsas.gestiondocumental.exception;

/**
 * Se produce cuando un usuario quiere cambiar su contraseña pero
 * escribe mal la contraseña actual. Se usa un error específico, en
 * vez de uno genérico, para que el sistema pueda contar ese intento
 * fallido y bloquear temporalmente a quien lo repita demasiadas veces.
 */
public class ContrasenaActualIncorrectaException extends BusinessException {

    public ContrasenaActualIncorrectaException() {
        super("La contraseña actual es incorrecta.");
    }
}
