package com.plantarsas.gestiondocumental.shared.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class FechaHoraUtcTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Test
    void ahoraDesde_conClockFijo_generaComponentesUtc() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-25T13:21:57.806725Z"), ZoneOffset.UTC);

        LocalDateTime almacenado = FechaHoraUtc.ahoraDesde(clock);

        assertThat(almacenado).isEqualTo(LocalDateTime.of(2026, 8, 25, 13, 21, 57, 806725000));
    }

    @Test
    void ahoraDesde_noDependeDeZonaDelSistema() {
        Instant instante = Instant.parse("2026-08-25T03:00:00Z");
        Clock clockUtc = Clock.fixed(instante, ZoneOffset.UTC);
        Clock clockBogota = Clock.fixed(instante, BOGOTA);

        assertThat(FechaHoraUtc.ahoraDesde(clockUtc))
                .isEqualTo(FechaHoraUtc.ahoraDesde(clockBogota));
    }

    @Test
    void aInstant_conLocalDateTimeUtcNaive_equivalenteAInstanteZ() {
        LocalDateTime almacenado = LocalDateTime.of(2026, 8, 25, 13, 21, 57);

        assertThat(FechaHoraUtc.aInstant(almacenado))
                .isEqualTo(Instant.parse("2026-08-25T13:21:57Z"));
    }

    @Test
    void inicioDiaPresentacionEnAlmacenamiento_convierteDiaColombiaAUtcNaive() {
        LocalDate dia = LocalDate.of(2026, 8, 25);

        assertThat(FechaHoraUtc.inicioDiaPresentacionEnAlmacenamiento(dia))
                .isEqualTo(LocalDateTime.of(2026, 8, 25, 5, 0));
    }

    @Test
    void inicioDiaSiguientePresentacionEnAlmacenamiento_esLimiteExclusivo() {
        LocalDate dia = LocalDate.of(2026, 8, 25);

        assertThat(FechaHoraUtc.inicioDiaSiguientePresentacionEnAlmacenamiento(dia))
                .isEqualTo(LocalDateTime.of(2026, 8, 26, 5, 0));
    }

    @Test
    void accionNocturnaEnBogota_perteneceAlMismoDiaCalendarioColombia() {
        Instant accion = LocalDate.of(2026, 8, 25)
                .atTime(22, 0)
                .atZone(BOGOTA)
                .toInstant();
        LocalDateTime almacenado = FechaHoraUtc.ahoraDesde(Clock.fixed(accion, ZoneOffset.UTC));

        assertThat(almacenado.toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 26));
        assertThat(FechaHoraUtc.aInstant(almacenado).atZone(BOGOTA).toLocalDate())
                .isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void normalizacionHistorica_localBogota_ejemploMatematico() {
        LocalDateTime historicoBogotaNaive = LocalDateTime.of(2026, 8, 20, 8, 0);
        LocalDateTime normalizado = historicoBogotaNaive
                .atZone(BOGOTA)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        assertThat(normalizado).isEqualTo(LocalDateTime.of(2026, 8, 20, 13, 0));
    }

    @Test
    void normalizacionHistorica_renderUtc_ejemploMatematico() {
        LocalDateTime historicoUtcNaive = LocalDateTime.of(2026, 8, 25, 13, 21);
        LocalDateTime normalizado = historicoUtcNaive
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        assertThat(normalizado).isEqualTo(LocalDateTime.of(2026, 8, 25, 13, 21));
    }
}
