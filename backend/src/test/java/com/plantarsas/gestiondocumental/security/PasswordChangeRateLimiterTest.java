package com.plantarsas.gestiondocumental.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordChangeRateLimiterTest {

    private static final Instant INICIO = Instant.parse("2026-01-01T12:00:00Z");
    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_SEGUNDOS = 3600;

    private RelojAjustable reloj;
    private PasswordChangeRateLimiter limiter;

    @BeforeEach
    void inicializar() {
        reloj = new RelojAjustable(INICIO);
        limiter = new PasswordChangeRateLimiter(reloj, MAX_INTENTOS, VENTANA_SEGUNDOS);
    }

    @Test
    void primerosFallos_noDebenBloquear() {
        Long usuarioId = 10L;

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            assertThat(limiter.estaBloqueado(usuarioId)).isFalse();
            limiter.registrarFallo(usuarioId);
        }
    }

    @Test
    void alAlcanzarElLimite_debeBloquear() {
        Long usuarioId = 10L;

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            limiter.registrarFallo(usuarioId);
        }

        assertThat(limiter.estaBloqueado(usuarioId)).isTrue();
    }

    @Test
    void usuarioA_noBloqueaUsuarioB() {
        Long usuarioA = 10L;
        Long usuarioB = 20L;

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            limiter.registrarFallo(usuarioA);
        }

        assertThat(limiter.estaBloqueado(usuarioA)).isTrue();
        assertThat(limiter.estaBloqueado(usuarioB)).isFalse();
    }

    @Test
    void limpiar_debeReiniciarElContador() {
        Long usuarioId = 10L;

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            limiter.registrarFallo(usuarioId);
        }
        assertThat(limiter.estaBloqueado(usuarioId)).isTrue();

        limiter.limpiar(usuarioId);

        assertThat(limiter.estaBloqueado(usuarioId)).isFalse();
        limiter.registrarFallo(usuarioId);
        assertThat(limiter.estaBloqueado(usuarioId)).isFalse();
    }

    @Test
    void trasVencerLaVentana_debeDejarDeEstarBloqueado() {
        Long usuarioId = 10L;

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            limiter.registrarFallo(usuarioId);
        }
        assertThat(limiter.estaBloqueado(usuarioId)).isTrue();

        reloj.avanzar(Duration.ofSeconds(VENTANA_SEGUNDOS + 1));

        assertThat(limiter.estaBloqueado(usuarioId)).isFalse();
    }

    @Test
    void constructor_debeRechazarParametrosInvalidos() {
        assertThatThrownBy(() -> new PasswordChangeRateLimiter(reloj, 0, VENTANA_SEGUNDOS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PasswordChangeRateLimiter(reloj, MAX_INTENTOS, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void respetaMaxAttemptsDelConstructor() {
        PasswordChangeRateLimiter corto = new PasswordChangeRateLimiter(reloj, 2, VENTANA_SEGUNDOS);

        corto.registrarFallo(3L);
        assertThat(corto.estaBloqueado(3L)).isFalse();
        corto.registrarFallo(3L);
        assertThat(corto.estaBloqueado(3L)).isTrue();
    }

    private static final class RelojAjustable extends Clock {

        private Instant instante;

        private RelojAjustable(Instant instante) {
            this.instante = instante;
        }

        private void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
