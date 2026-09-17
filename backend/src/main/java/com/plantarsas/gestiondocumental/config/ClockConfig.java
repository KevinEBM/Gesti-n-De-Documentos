package com.plantarsas.gestiondocumental.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provee la hora actual del sistema como un componente reutilizable,
 * en vez de que cada clase la obtenga por su cuenta, para que las
 * pruebas automatizadas puedan simular fechas concretas y así
 * comprobar comportamientos que dependen del tiempo, como la
 * expiración de un token o el bloqueo temporal de intentos de inicio
 * de sesión.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
