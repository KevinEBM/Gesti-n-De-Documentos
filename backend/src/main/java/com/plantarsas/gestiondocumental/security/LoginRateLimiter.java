package com.plantarsas.gestiondocumental.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LoginRateLimiter {

    private static final int INTERVALO_LIMPIEZA = 256;

    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();
    private final AtomicLong solicitudesProcesadas = new AtomicLong();

    public LoginRateLimiter(
            Clock clock,
            @Value("${security.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${security.login-rate-limit.window-seconds:60}") long windowSeconds
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

        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /**
     * Registra un intento de login para la IP indicada.
     *
     * @return {@code true} si la solicitud puede continuar; {@code false} si se excedió el límite
     */
    public boolean registrarIntento(String clientIp) {
        Instant now = clock.instant();
        String ip = normalizarIp(clientIp);

        Window ventana = attempts.compute(ip, (clave, existente) -> {
            if (existente == null || ventanaVencida(existente, now)) {
                return new Window(1, now);
            }
            return new Window(existente.count + 1, existente.windowStart);
        });

        limpiarEntradasVencidasOportunistamente(now);

        return ventana.count <= maxAttempts;
    }

    private boolean ventanaVencida(Window ventana, Instant now) {
        return now.isAfter(ventana.windowStart.plus(window));
    }

    private void limpiarEntradasVencidasOportunistamente(Instant now) {
        if (solicitudesProcesadas.incrementAndGet() % INTERVALO_LIMPIEZA != 0) {
            return;
        }

        attempts.entrySet().removeIf(entry -> ventanaVencida(entry.getValue(), now));
    }

    private static String normalizarIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "desconocida";
        }
        return clientIp.trim();
    }

    private record Window(int count, Instant windowStart) {
    }
}
