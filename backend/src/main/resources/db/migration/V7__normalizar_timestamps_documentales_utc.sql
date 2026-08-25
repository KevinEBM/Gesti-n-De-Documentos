-- ============================================================
-- V7: Normalizar timestamps documentales a convención UTC naive.
--
-- Antes de esta migración los valores TIMESTAMP WITHOUT TIME ZONE
-- representaban hora de pared según la zona de sesión PostgreSQL
-- del entorno que escribió el dato:
--   * desarrollo local: America/Bogota
--   * Render: UTC
--
-- A partir de V7 la convención única es: componentes de fecha/hora en UTC.
--
-- Transformación (una sola ejecución vía Flyway):
--   (columna AT TIME ZONE current_setting('TIMEZONE')) AT TIME ZONE 'UTC'
--
-- Ejemplos:
--   Bogota 2026-08-20 08:00 -> 2026-08-20 13:00 UTC naive
--   UTC    2026-08-25 13:21 -> 2026-08-25 13:21 UTC naive (sin cambio)
-- ============================================================

UPDATE documentos
SET fecha_creacion = (fecha_creacion AT TIME ZONE current_setting('TIMEZONE')) AT TIME ZONE 'UTC',
    fecha_actualizacion = (fecha_actualizacion AT TIME ZONE current_setting('TIMEZONE')) AT TIME ZONE 'UTC'
WHERE fecha_creacion IS NOT NULL
   OR fecha_actualizacion IS NOT NULL;

UPDATE versiones_documento
SET fecha_publicacion = (fecha_publicacion AT TIME ZONE current_setting('TIMEZONE')) AT TIME ZONE 'UTC'
WHERE fecha_publicacion IS NOT NULL;

UPDATE documento_area
SET fecha_asignacion = (fecha_asignacion AT TIME ZONE current_setting('TIMEZONE')) AT TIME ZONE 'UTC'
WHERE fecha_asignacion IS NOT NULL;
