package com.plantarsas.gestiondocumental.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class LoginRateLimiter {

    private final InMemoryAttemptLimiter limiter;

    public LoginRateLimiter(
            Clock clock,
            @Value("${security.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${security.login-rate-limit.window-seconds:300}") long windowSeconds
    ) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "security.login-rate-limit.max-attempts debe ser mayor que cero"
            );
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException(
                    "security.login-rate-limit.window-seconds debe ser mayor que cero"
            );
        }
        this.limiter = new InMemoryAttemptLimiter(clock, maxAttempts, windowSeconds);
    }

    public boolean estaBloqueado(String clientIp) {
        return limiter.estaBloqueado(normalizarIp(clientIp));
    }

    public void registrarFallo(String clientIp) {
        limiter.registrar(normalizarIp(clientIp));
    }

    public void limpiar(String clientIp) {
        limiter.limpiar(normalizarIp(clientIp));
    }

    private static String normalizarIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "desconocida";
        }
        return clientIp.trim();
    }
}
