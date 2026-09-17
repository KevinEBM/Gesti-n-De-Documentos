package com.plantarsas.gestiondocumental.shared.enums;

/**
 * Los tres tipos de usuario que existen en el sistema: administrador,
 * jefe de área y administrativo. De este rol depende qué puede hacer
 * cada usuario y qué información puede ver; por ejemplo, un
 * administrador ve todo el sistema, mientras que los demás roles solo
 * ven lo de su propia área.
 */
public enum RolEnum {
    ADMINISTRADOR,
    JEFE_AREA,
    ADMINISTRATIVO
}

