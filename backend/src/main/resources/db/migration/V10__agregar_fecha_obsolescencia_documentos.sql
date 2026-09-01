-- ============================================================
-- V10: retención de documentos obsoletos.
--
-- Regla de negocio: un documento nunca se elimina automáticamente.
-- Al pasar a OBSOLETO se registra el instante exacto de la transición
-- y a partir de ahí corre un plazo de 2 años completos. Cumplido el
-- plazo el documento queda "apto para eliminación" y únicamente un
-- ADMINISTRADOR puede confirmar la eliminación definitiva.
--
-- Convención temporal (fijada en V7): TIMESTAMP WITHOUT TIME ZONE con
-- componentes en UTC. fecha_obsolescencia sigue exactamente la misma
-- convención; la escribe el backend con FechaHoraUtc.ahoraDesde(clock).
--
-- La columna es nullable a propósito: solo los documentos que están
-- actualmente en OBSOLETO tienen fecha. Salir de OBSOLETO la limpia y
-- volver a OBSOLETO abre un período nuevo; nunca se reutiliza la fecha
-- de una obsolescencia anterior.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Columna
-- ------------------------------------------------------------
ALTER TABLE documentos
ADD COLUMN fecha_obsolescencia TIMESTAMP;

-- ------------------------------------------------------------
-- 2. Backfill de los documentos que ya están en OBSOLETO
--
-- Para un documento OBSOLETO, fecha_actualizacion ES el instante en
-- que se marcó obsoleto, no una aproximación:
--   * cambiarEstado() escribe fecha_actualizacion en la transición;
--   * estando OBSOLETO el backend rechaza editar metadatos
--     (DocumentoEstado.permiteEditarPublicacion) y publicar nuevas
--     versiones (DocumentoEstado.permitePublicarNuevaVersion);
--   * una transición OBSOLETO -> OBSOLETO no escribe nada.
-- Por tanto ninguna operación posterior pudo mover esa fecha.
--
-- V7 ya normalizó fecha_actualizacion a UTC naive, así que el valor
-- copiado respeta la convención de la columna nueva.
-- ------------------------------------------------------------
UPDATE documentos
SET fecha_obsolescencia = fecha_actualizacion
WHERE estado = 'OBSOLETO'
  AND fecha_obsolescencia IS NULL;

-- ------------------------------------------------------------
-- 3. Invariante: la fecha existe si y solo si el estado es OBSOLETO
--
-- Mismo patrón de CHECK que ya usan chk_documentos_estado (V2) y
-- chk_documentos_alcance (V5). Impide que un documento quede OBSOLETO
-- sin plazo, o que conserve un plazo tras ser reactivado.
-- ------------------------------------------------------------
ALTER TABLE documentos
ADD CONSTRAINT chk_documentos_fecha_obsolescencia
CHECK (
    (estado = 'OBSOLETO' AND fecha_obsolescencia IS NOT NULL)
    OR (estado <> 'OBSOLETO' AND fecha_obsolescencia IS NULL)
);
