package com.plantarsas.gestiondocumental.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class PasswordChangeRateLimiter {

    private final InMemoryAttemptLimiter limiter;

    public PasswordChangeRateLimiter(
            Clock clock,
            @Value("${security.password-change-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${security.password-change-rate-limit.window-seconds:3600}") long windowSeconds
    ) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "security.password-change-rate-limit.max-attempts debe ser mayor que cero"
            );
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException(
                    "security.password-change-rate-limit.window-seconds debe ser mayor que cero"
            );
        }
        this.limiter = new InMemoryAttemptLimiter(clock, maxAttempts, windowSeconds);
    }

    public boolean estaBloqueado(Long usuarioId) {
        return limiter.estaBloqueado(clave(usuarioId));
    }

    public void registrarFallo(Long usuarioId) {
        limiter.registrar(clave(usuarioId));
    }

    public void limpiar(Long usuarioId) {
        limiter.limpiar(clave(usuarioId));
    }

    private static String clave(Long usuarioId) {
        if (usuarioId == null) {
            return "desconocido";
        }
        return "user:" + usuarioId;
    }
}
