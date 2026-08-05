-- ============================================================
-- V3: renombrar categorias a subprogramas, asociarlas a un area
--     obligatoria, y renombrar documentos.categoria_id a
--     subprograma_id.
-- No modifica V1 ni V2.
-- No agrega area_responsable_id: el area responsable del
-- documento se representara mas adelante mediante la relacion
-- existente documento_area.es_principal, no con una columna
-- nueva en documentos.
-- ============================================================

-- ------------------------------------------------------------
-- 0. Guardas de seguridad: no continuar si ya hay datos reales.
--    No se asignan areas por defecto ni se inventan relaciones.
-- ------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM categorias) THEN
        RAISE EXCEPTION
            'V3 abortada: la tabla categorias ya contiene filas. No se asignaran areas por defecto. Resuelva manualmente antes de reintentar.';
    END IF;

    IF EXISTS (SELECT 1 FROM documentos) THEN
        RAISE EXCEPTION
            'V3 abortada: la tabla documentos ya contiene filas. No se puede renombrar categoria_id sin verificacion manual.';
    END IF;
END $$;

-- ------------------------------------------------------------
-- 1. Renombrar la tabla
-- ------------------------------------------------------------
ALTER TABLE categorias RENAME TO subprogramas;

-- ------------------------------------------------------------
-- 2. Renombrar la PK unicamente si su nombre por defecto exacto
--    existe todavia (categorias_pkey). Si no existe con ese
--    nombre exacto, este paso se omite sin fallar.
-- ------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'categorias_pkey'
          AND conrelid = 'subprogramas'::regclass
    ) THEN
        ALTER TABLE subprogramas RENAME CONSTRAINT categorias_pkey TO subprogramas_pkey;
    END IF;
END $$;

-- ------------------------------------------------------------
-- 3. Renombrar la secuencia de identity unicamente si su
--    nombre por defecto exacto existe todavia
--    (categorias_id_seq). Las secuencias no se renombran
--    automaticamente al renombrar la tabla.
-- ------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_class
        WHERE relkind = 'S'
          AND relname = 'categorias_id_seq'
          AND relnamespace = 'public'::regnamespace
    ) THEN
        ALTER SEQUENCE categorias_id_seq RENAME TO subprogramas_id_seq;
    END IF;
END $$;

-- ------------------------------------------------------------
-- 4. Cada subprograma pertenece obligatoriamente a un area
-- ------------------------------------------------------------
ALTER TABLE subprogramas ADD COLUMN area_id BIGINT NOT NULL;
ALTER TABLE subprogramas
    ADD CONSTRAINT fk_subprogramas_area
        FOREIGN KEY (area_id) REFERENCES areas (id);

-- ------------------------------------------------------------
-- 5. Unicidad de nombre por area (no global). Se permite el
--    mismo nombre en areas diferentes.
-- ------------------------------------------------------------
DROP INDEX IF EXISTS uq_categorias_nombre_lower;

CREATE UNIQUE INDEX uq_subprogramas_area_nombre_lower
    ON subprogramas (area_id, LOWER(nombre));

-- ------------------------------------------------------------
-- 6. Renombrar la columna y la constraint en documentos
-- ------------------------------------------------------------
ALTER TABLE documentos RENAME COLUMN categoria_id TO subprograma_id;
ALTER TABLE documentos
    RENAME CONSTRAINT fk_documentos_categoria TO fk_documentos_subprograma;
