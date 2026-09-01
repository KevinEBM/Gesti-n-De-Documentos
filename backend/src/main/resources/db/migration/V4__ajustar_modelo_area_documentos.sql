-- ============================================================
-- V4: eliminar documentos.es_global, exigir exactamente una
--     fila (siempre principal) en documento_area por documento,
--     e indices de consulta para documentos/versiones.
-- No modifica V1, V2 ni V3.
-- Politica fail-fast: no se corrigen ni eliminan datos existentes
-- de forma automatica. Si la tabla documento_area ya contuviera
-- filas que violen las restricciones nuevas, las sentencias
-- ADD CONSTRAINT de abajo fallan por si solas y detienen la
-- migracion. No se usa IF EXISTS en los DROP porque los nombres
-- de los objetos ya fueron verificados exactos contra V2 (no hay
-- incertidumbre que justifique ocultar una diferencia de esquema).
-- ============================================================

-- ------------------------------------------------------------
-- Diagnostico previo (documentacion, NO se ejecuta como parte de
-- esta migracion). Deben revisarse manualmente antes de aplicar
-- V4 en cualquier entorno con datos reales:
--
-- SELECT documento_id, COUNT(*)
-- FROM documento_area
-- GROUP BY documento_id
-- HAVING COUNT(*) > 1;
--
-- SELECT COUNT(*)
-- FROM documento_area
-- WHERE es_principal IS DISTINCT FROM TRUE;
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 1. documentos: eliminar es_global (fuera del alcance actual)
-- ------------------------------------------------------------
ALTER TABLE documentos DROP COLUMN es_global;

-- ------------------------------------------------------------
-- 2. documento_area: exactamente una fila (principal) por
--    documento. Se reemplazan las restricciones anteriores,
--    pensadas para multiples areas por documento, por una unica
--    restriccion sobre documento_id.
-- ------------------------------------------------------------
DROP INDEX uq_documento_area_principal;
ALTER TABLE documento_area DROP CONSTRAINT uq_documento_area;

ALTER TABLE documento_area
    ADD CONSTRAINT uq_documento_area_documento UNIQUE (documento_id);

ALTER TABLE documento_area
    ADD CONSTRAINT chk_documento_area_es_principal
        CHECK (es_principal = TRUE);

-- ------------------------------------------------------------
-- 3. Indices para consultas frecuentes.
--    Se usa un indice compuesto (area_id, documento_id) en vez
--    de un indice simple sobre area_id: cubre igual de bien las
--    busquedas "documentos de un area" y ademas permite resolver
--    esas consultas como index-only scan cuando solo se necesita
--    el documento_id asociado, sin volver a la tabla.
-- ------------------------------------------------------------
CREATE INDEX ix_documento_area_area_documento
    ON documento_area (area_id, documento_id);

CREATE INDEX ix_documentos_subprograma_id
    ON documentos (subprograma_id);

CREATE INDEX ix_documentos_tipo_documento_id
    ON documentos (tipo_documento_id);

CREATE INDEX ix_documentos_creado_por
    ON documentos (creado_por);

CREATE INDEX ix_versiones_documento_fecha_publicacion
    ON versiones_documento (fecha_publicacion);
