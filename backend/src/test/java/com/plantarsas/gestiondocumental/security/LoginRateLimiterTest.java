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

class LoginRateLimiterTest {

    private static final Instant INICIO = Instant.parse("2026-01-01T12:00:00Z");
    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_SEGUNDOS = 60;

    private RelojAjustable reloj;
    private LoginRateLimiter limiter;

    @BeforeEach
    void inicializar() {
        reloj = new RelojAjustable(INICIO);
        limiter = new LoginRateLimiter(reloj, MAX_INTENTOS, VENTANA_SEGUNDOS);
    }

    @Test
    void primerIntento_debePermitirse() {
        assertThat(limiter.registrarIntento("192.168.1.1")).isTrue();
    }

    @Test
    void cincoIntentosDentroDeLaVentana_debenPermitirse() {
        String ip = "192.168.1.1";

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            assertThat(limiter.registrarIntento(ip))
                    .as("intento %d", intento)
                    .isTrue();
        }
    }

    @Test
    void sextoIntentoDesdeLaMismaIp_debeRechazarse() {
        String ip = "192.168.1.1";

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            assertThat(limiter.registrarIntento(ip)).isTrue();
        }

        assertThat(limiter.registrarIntento(ip)).isFalse();
    }

    @Test
    void otraIp_debeConservarSuPropioContador() {
        String ipBloqueada = "10.0.0.1";
        String ipLibre = "10.0.0.2";

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            assertThat(limiter.registrarIntento(ipBloqueada)).isTrue();
        }
        assertThat(limiter.registrarIntento(ipBloqueada)).isFalse();

        assertThat(limiter.registrarIntento(ipLibre)).isTrue();
    }

    @Test
    void trasVencerLaVentana_debeVolverAPermitirSolicitudes() {
        String ip = "192.168.1.1";

        for (int intento = 0; intento < MAX_INTENTOS; intento++) {
            assertThat(limiter.registrarIntento(ip)).isTrue();
        }
        assertThat(limiter.registrarIntento(ip)).isFalse();

        reloj.avanzar(Duration.ofSeconds(VENTANA_SEGUNDOS + 1));

        assertThat(limiter.registrarIntento(ip)).isTrue();
    }

    @Test
    void ipNulaOVacia_debeNormalizarseSinFallar() {
        assertThat(limiter.registrarIntento(null)).isTrue();
        assertThat(limiter.registrarIntento("   ")).isTrue();
    }

    @Test
    void parametrosInvalidos_debenRechazarseEnConstruccion() {
        assertThatThrownBy(() -> new LoginRateLimiter(reloj, 0, VENTANA_SEGUNDOS))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new LoginRateLimiter(reloj, MAX_INTENTOS, 0))
                .isInstanceOf(IllegalArgumentException.class);
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
