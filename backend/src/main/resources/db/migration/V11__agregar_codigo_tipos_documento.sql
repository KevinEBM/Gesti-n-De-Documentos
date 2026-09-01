-- ============================================================
-- V11: código parametrizable en tipos_documento.
--
-- El código es obligatorio, se normaliza (trim + uppercase) en aplicación
-- y PUEDE repetirse entre tipos distintos. ODE se asigna a:
--   Plantilla, Documentos Externos, Imágenes
--
-- NO se crea UNIQUE(codigo) ni UNIQUE(LOWER(codigo)).
-- El identificador de entidad sigue siendo id.
--
-- No se modifican IDs, nombres, activo ni otros campos.
-- No se eliminan ni recrean tipos.
-- No se inventan códigos para registros desconocidos: si un Tipo no
-- coincide con el mapeo aprobado, la migración FALLA.
-- ============================================================

ALTER TABLE tipos_documento
    ADD COLUMN codigo VARCHAR(20);

UPDATE tipos_documento t
SET codigo = m.codigo
FROM (
    VALUES
        ('Manual', 'MA'),
        ('Programa', 'PG'),
        ('Proceso', 'PC'),
        ('Procedimiento', 'PD'),
        ('Política', 'PL'),
        ('Reglamento', 'RT'),
        ('Caracterización', 'CR'),
        ('Instructivo', 'IN'),
        ('Protocolo', 'PT'),
        ('Formato', 'FO'),
        ('Ficha Técnica', 'FT'),
        ('Diagrama', 'DG'),
        ('Otros Documentos', 'OD'),
        ('Plantilla', 'ODE'),
        ('Documentos Externos', 'ODE'),
        ('Imágenes', 'ODE')
) AS m(nombre, codigo)
WHERE translate(LOWER(t.nombre), 'áéíóúü', 'aeiouu')
    = translate(LOWER(m.nombre), 'áéíóúü', 'aeiouu');

DO $$
DECLARE
    desconocidos TEXT;
BEGIN
    SELECT string_agg(t.nombre, ', ' ORDER BY t.nombre)
    INTO desconocidos
    FROM tipos_documento t
    WHERE t.codigo IS NULL;

    IF desconocidos IS NOT NULL THEN
        RAISE EXCEPTION
            'V11: tipos de documento sin código en el mapeo aprobado: %. No se inventan códigos; defina el mapeo en una migración posterior.',
            desconocidos;
    END IF;
END $$;

ALTER TABLE tipos_documento
    ALTER COLUMN codigo SET NOT NULL;
