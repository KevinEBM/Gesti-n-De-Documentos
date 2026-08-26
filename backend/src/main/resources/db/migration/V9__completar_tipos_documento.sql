-- ============================================================
-- V9: completar el catálogo institucional de tipos de documento.
--
-- V6 cargó 13 tipos, entre ellos 'Otros Documentos'. Faltan tres
-- que la parametrización necesita ofrecer al administrador:
--   Plantilla, Documentos Externos, Imágenes
--
-- Reglas:
--   - 'Otros Documentos' NO se inserta: ya existe desde V6.
--   - Migración autosuficiente: no usa flyway_normalizar_nombre_catalogo
--     (V6 la elimina al finalizar) ni extensiones como unaccent.
--   - Criterio ÚNICO de comparación en toda la migración:
--         translate(LOWER(nombre), 'áéíóúü', 'aeiouu')
--     Es más laxo que el índice uq_tipos_documento_nombre_lower, que
--     compara solo LOWER(nombre) y por tanto no equipara 'Imágenes'
--     con 'Imagenes'. Usar el mismo criterio en la inserción y en las
--     guardas evita que 'Imagenes' sin tilde provoque un duplicado
--     semántico o un falso faltante.
--   - El nombre canónico persistido es 'Imágenes', con tilde.
--   - No se inventan abreviaturas documentales: estos tipos no entran
--     en la nomenclatura automática, solo en parametrización.
--   - No se modifica ninguna fila existente.
-- ============================================================

INSERT INTO tipos_documento (nombre, descripcion, activo)
SELECT v.nombre, NULL, TRUE
FROM (
    VALUES
        ('Plantilla'),
        ('Documentos Externos'),
        ('Imágenes')
) AS v(nombre)
WHERE NOT EXISTS (
    SELECT 1
    FROM tipos_documento t
    WHERE translate(LOWER(t.nombre), 'áéíóúü', 'aeiouu')
        = translate(LOWER(v.nombre), 'áéíóúü', 'aeiouu')
);

-- ------------------------------------------------------------
-- Guardas: el catálogo debe quedar completo y sin duplicados,
-- evaluadas con el mismo criterio de normalización que la inserción.
-- ------------------------------------------------------------
DO $$
DECLARE
    faltantes TEXT;
    duplicados TEXT;
BEGIN
    SELECT string_agg(v.nombre, ', ')
    INTO faltantes
    FROM (
        VALUES
            ('Plantilla'),
            ('Otros Documentos'),
            ('Documentos Externos'),
            ('Imágenes')
    ) AS v(nombre)
    WHERE NOT EXISTS (
        SELECT 1
        FROM tipos_documento t
        WHERE translate(LOWER(t.nombre), 'áéíóúü', 'aeiouu')
            = translate(LOWER(v.nombre), 'áéíóúü', 'aeiouu')
    );

    IF faltantes IS NOT NULL THEN
        RAISE EXCEPTION 'V9: faltan tipos de documento requeridos: %', faltantes;
    END IF;

    SELECT string_agg(d.nombre_normalizado, ', ')
    INTO duplicados
    FROM (
        SELECT translate(LOWER(nombre), 'áéíóúü', 'aeiouu') AS nombre_normalizado
        FROM tipos_documento
        GROUP BY translate(LOWER(nombre), 'áéíóúü', 'aeiouu')
        HAVING COUNT(*) > 1
    ) AS d;

    IF duplicados IS NOT NULL THEN
        RAISE EXCEPTION 'V9: tipos de documento duplicados: %', duplicados;
    END IF;
END $$;
