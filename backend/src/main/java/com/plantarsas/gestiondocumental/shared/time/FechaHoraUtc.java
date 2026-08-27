package com.plantarsas.gestiondocumental.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Convención temporal del módulo documental:
 * almacenamiento naive = componentes UTC; presentación = America/Bogota.
 */
public final class FechaHoraUtc {

    public static final ZoneId ZONA_PRESENTACION = ZoneId.of("America/Bogota");
    public static final ZoneOffset OFFSET_ALMACENAMIENTO = ZoneOffset.UTC;

    private FechaHoraUtc() {
    }

    public static LocalDateTime ahoraDesde(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), OFFSET_ALMACENAMIENTO);
    }

    public static Instant aInstant(LocalDateTime almacenadoUtc) {
        return almacenadoUtc.toInstant(OFFSET_ALMACENAMIENTO);
    }

    /** Variante para columnas opcionales, como la fecha de obsolescencia. */
    public static Instant aInstantONulo(LocalDateTime almacenadoUtc) {
        return almacenadoUtc == null ? null : aInstant(almacenadoUtc);
    }

    /** Inicio del día calendario Colombia expresado como UTC naive para comparar en BD. */
    public static LocalDateTime inicioDiaPresentacionEnAlmacenamiento(LocalDate dia) {
        return dia.atStartOfDay(ZONA_PRESENTACION)
                .withZoneSameInstant(OFFSET_ALMACENAMIENTO)
                .toLocalDateTime();
    }

    /** Límite exclusivo: inicio del día siguiente en Colombia, como UTC naive. */
    public static LocalDateTime inicioDiaSiguientePresentacionEnAlmacenamiento(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay(ZONA_PRESENTACION)
                .withZoneSameInstant(OFFSET_ALMACENAMIENTO)
                .toLocalDateTime();
    }
}
