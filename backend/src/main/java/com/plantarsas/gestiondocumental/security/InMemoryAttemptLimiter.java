package com.plantarsas.gestiondocumental.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contador en memoria por clave, con ventana deslizante fija desde el primer intento.
 * Lo reutilizan el rate limit de login (clave = IP) y el de cambio de contraseña (clave = usuario).
 */
final class InMemoryAttemptLimiter {

    private static final int INTERVALO_LIMPIEZA = 256;

    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();
    private final AtomicLong solicitudesProcesadas = new AtomicLong();

    InMemoryAttemptLimiter(Clock clock, int maxAttempts, long windowSeconds) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts debe ser mayor que cero");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds debe ser mayor que cero");
        }
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /**
     * Incrementa el contador y retorna {@code true} si el intento sigue dentro del límite.
     */
    boolean registrar(String clave) {
        Instant now = clock.instant();
        Window ventana = attempts.compute(clave, (ignorada, existente) -> {
            if (existente == null || ventanaVencida(existente, now)) {
                return new Window(1, now);
            }
            return new Window(existente.count + 1, existente.windowStart);
        });
        limpiarEntradasVencidasOportunistamente(now);
        return ventana.count <= maxAttempts;
    }

    boolean estaBloqueado(String clave) {
        Window ventana = attempts.get(clave);
        if (ventana == null) {
            return false;
        }
        Instant now = clock.instant();
        if (ventanaVencida(ventana, now)) {
            return false;
        }
        return ventana.count >= maxAttempts;
    }

    void limpiar(String clave) {
        attempts.remove(clave);
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

    private record Window(int count, Instant windowStart) {
    }
}
