-- agregar columna
ALTER TABLE documentos
ADD COLUMN alcance VARCHAR(20);

-- backfill
UPDATE documentos
SET alcance = 'AREA_RESPONSABLE'
WHERE alcance IS NULL;

-- obligatoriedad
ALTER TABLE documentos
ALTER COLUMN alcance SET NOT NULL;

-- valores permitidos
ALTER TABLE documentos
ADD CONSTRAINT chk_documentos_alcance
CHECK (alcance IN ('AREA_RESPONSABLE', 'AREAS_ESPECIFICAS', 'GLOBAL'));

-- retirar restricciones actuales de V4
ALTER TABLE documento_area
DROP CONSTRAINT uq_documento_area_documento;

ALTER TABLE documento_area
DROP CONSTRAINT chk_documento_area_es_principal;

-- impedir duplicar area en el mismo documento
ALTER TABLE documento_area
ADD CONSTRAINT uq_documento_area_documento_area
UNIQUE (documento_id, area_id);

-- maximo una principal por documento
CREATE UNIQUE INDEX uq_documento_area_principal
ON documento_area (documento_id)
WHERE es_principal = TRUE;
