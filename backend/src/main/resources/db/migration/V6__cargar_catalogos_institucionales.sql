-- ============================================================
-- V6: precarga idempotente de catálogos institucionales
--     (11 áreas, 40 subprogramas, 13 tipos de documento)
--
-- Reglas:
--   - IDs generados por PostgreSQL (sin IDs fijos)
--   - descripcion = NULL para registros nuevos
--   - activo = TRUE solo en inserts nuevos
--   - Sin DELETE / TRUNCATE / UPDATE masivo
--   - Comparación por nombre normalizado (tildes + espacios)
--   - Colisión de codigo de área con nombre distinto → abortar
--   - Ambigüedad de área por nombre normalizado → abortar
-- ============================================================

-- Función auxiliar exclusiva de esta migración (nombre específico Flyway).
-- No usar CREATE OR REPLACE: abortar si ya existiera un objeto homónimo.
DO $$
BEGIN
    IF to_regprocedure('flyway_normalizar_nombre_catalogo(text)') IS NOT NULL THEN
        RAISE EXCEPTION
            'V6 abortada: la funcion auxiliar flyway_normalizar_nombre_catalogo(text) ya existe. Resuelva manualmente antes de reintentar.';
    END IF;
END $$;

CREATE FUNCTION flyway_normalizar_nombre_catalogo(p_texto TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT translate(
        regexp_replace(trim(lower(coalesce(p_texto, ''))), '\s+', ' ', 'g'),
        'áéíóúüñ',
        'aeiouun'
    );
$$;

-- Catálogo institucional de áreas (reutilizado en validaciones e inserts).
-- Normalización equivalente a:
--   translate(regexp_replace(trim(lower(nombre)), '\s+', ' ', 'g'), 'áéíóúüñ', 'aeiouun')

-- ------------------------------------------------------------
-- 0. Detectar colisión real de codigo de área
-- ------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM areas a
        INNER JOIN (
            VALUES
                ('GHUM',  'Gestión Humana'),
                ('GAMB',  'Gestión Ambiental'),
                ('GCAL',  'Gestión de Calidad'),
                ('GLOG',  'Gestión Logística'),
                ('GCOM',  'Gestión Comercial'),
                ('GPROD', 'Gestión de Producción'),
                ('GMANT', 'Gestión de Mantenimiento'),
                ('GSEGF', 'Gestión de Seguridad Física'),
                ('GTICS', 'Gestión TICs'),
                ('GFIN',  'Gestión Financiera'),
                ('GCOMP', 'Gestión de Compras')
        ) AS v(codigo, nombre) ON lower(a.codigo) = lower(v.codigo)
        WHERE flyway_normalizar_nombre_catalogo(a.nombre)
              <> flyway_normalizar_nombre_catalogo(v.nombre)
    ) THEN
        RAISE EXCEPTION
            'V6 abortada: existe un codigo de area institucional asignado a otra area distinta. Resuelva manualmente antes de reintentar.';
    END IF;
END $$;

-- ------------------------------------------------------------
-- 0b. Detectar ambigüedad: más de un área por nombre normalizado
-- ------------------------------------------------------------
DO $$
DECLARE
    area_ambigua TEXT;
BEGIN
    SELECT v.nombre
    INTO area_ambigua
    FROM (
        VALUES
            ('Gestión Humana'),
            ('Gestión Ambiental'),
            ('Gestión de Calidad'),
            ('Gestión Logística'),
            ('Gestión Comercial'),
            ('Gestión de Producción'),
            ('Gestión de Mantenimiento'),
            ('Gestión de Seguridad Física'),
            ('Gestión TICs'),
            ('Gestión Financiera'),
            ('Gestión de Compras')
    ) AS v(nombre)
    INNER JOIN areas a
        ON flyway_normalizar_nombre_catalogo(a.nombre)
           = flyway_normalizar_nombre_catalogo(v.nombre)
    GROUP BY v.nombre, flyway_normalizar_nombre_catalogo(v.nombre)
    HAVING COUNT(a.id) > 1
    LIMIT 1;

    IF area_ambigua IS NOT NULL THEN
        RAISE EXCEPTION
            'V6 abortada: catalogo institucional ambiguo: existen varias areas equivalentes para "%". Debe corregirse la informacion existente antes de aplicar V6.',
            area_ambigua;
    END IF;
END $$;

-- ------------------------------------------------------------
-- 1. Áreas (códigos técnicos internos del sistema)
-- ------------------------------------------------------------
INSERT INTO areas (codigo, nombre, descripcion, activo)
SELECT v.codigo, v.nombre, NULL, TRUE
FROM (
    VALUES
        ('GHUM',  'Gestión Humana'),
        ('GAMB',  'Gestión Ambiental'),
        ('GCAL',  'Gestión de Calidad'),
        ('GLOG',  'Gestión Logística'),
        ('GCOM',  'Gestión Comercial'),
        ('GPROD', 'Gestión de Producción'),
        ('GMANT', 'Gestión de Mantenimiento'),
        ('GSEGF', 'Gestión de Seguridad Física'),
        ('GTICS', 'Gestión TICs'),
        ('GFIN',  'Gestión Financiera'),
        ('GCOMP', 'Gestión de Compras')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1
    FROM areas a
    WHERE flyway_normalizar_nombre_catalogo(a.nombre)
          = flyway_normalizar_nombre_catalogo(v.nombre)
);

-- ------------------------------------------------------------
-- 1b. Cada área institucional debe resolver a exactamente una fila
-- ------------------------------------------------------------
DO $$
DECLARE
    area_problema TEXT;
    cantidad_areas BIGINT;
BEGIN
    SELECT v.nombre, COUNT(a.id)
    INTO area_problema, cantidad_areas
    FROM (
        VALUES
            ('Gestión Humana'),
            ('Gestión Ambiental'),
            ('Gestión de Calidad'),
            ('Gestión Logística'),
            ('Gestión Comercial'),
            ('Gestión de Producción'),
            ('Gestión de Mantenimiento'),
            ('Gestión de Seguridad Física'),
            ('Gestión TICs'),
            ('Gestión Financiera'),
            ('Gestión de Compras')
    ) AS v(nombre)
    LEFT JOIN areas a
        ON flyway_normalizar_nombre_catalogo(a.nombre)
           = flyway_normalizar_nombre_catalogo(v.nombre)
    GROUP BY v.nombre, flyway_normalizar_nombre_catalogo(v.nombre)
    HAVING COUNT(a.id) <> 1
    LIMIT 1;

    IF area_problema IS NOT NULL THEN
        IF cantidad_areas > 1 THEN
            RAISE EXCEPTION
                'V6 abortada: catalogo institucional ambiguo: existen varias areas equivalentes para "%". Debe corregirse la informacion existente antes de aplicar V6.',
                area_problema;
        ELSE
            RAISE EXCEPTION
                'V6 abortada: falta al menos un area institucional requerida para asociar subprogramas: "%".',
                area_problema;
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------------
-- 2. Subprogramas (40 institucionales; area_id por nombre)
-- ------------------------------------------------------------
INSERT INTO subprogramas (nombre, descripcion, area_id, activo)
SELECT v.subproceso_nombre, NULL, a.id, TRUE
FROM (
    VALUES
        -- Gestión Humana (5)
        ('Gestión Humana', 'Capacitación y Desarrollo'),
        ('Gestión Humana', 'Gestión Humana'),
        ('Gestión Humana', 'Salud y Seguridad en el Trabajo'),
        ('Gestión Humana', 'Sustancias Químicas'),
        ('Gestión Humana', 'Programa de Respeto, Convivencia e Inclusión'),

        -- Gestión Ambiental (6)
        ('Gestión Ambiental', 'Calidad del Agua Potable'),
        ('Gestión Ambiental', 'Control de Plagas'),
        ('Gestión Ambiental', 'Control de Residuos Líquidos'),
        ('Gestión Ambiental', 'Control de Residuos Sólidos'),
        ('Gestión Ambiental', 'Limpieza y Desinfección'),
        ('Gestión Ambiental', 'Gestión Ambiental'),

        -- Gestión de Calidad (14)
        ('Gestión de Calidad', 'Auditoría Interna'),
        ('Gestión de Calidad', 'Buenas Prácticas Higiénicas'),
        ('Gestión de Calidad', 'Control de Alérgenos'),
        ('Gestión de Calidad', 'Gestión Documental'),
        ('Gestión de Calidad', 'Gestión de la Calidad'),
        ('Gestión de Calidad', 'Material Extraño'),
        ('Gestión de Calidad', 'Plan de Muestreo'),
        ('Gestión de Calidad', 'Recall'),
        ('Gestión de Calidad', 'Sistemas Integrados de Gestión'),
        ('Gestión de Calidad', 'Trazabilidad'),
        ('Gestión de Calidad', 'Programa de Producto No Conforme'),
        ('Gestión de Calidad', 'Desarrollo e Innovación'),
        ('Gestión de Calidad', 'Proceso de Mejora Continua'),
        ('Gestión de Calidad', 'Programa de Peticiones Quejas y Reclamos'),

        -- Gestión Logística (4)
        ('Gestión Logística', 'Almacén y Abastecimiento de Insumos'),
        ('Gestión Logística', 'Logística'),
        ('Gestión Logística', 'Transporte'),
        ('Gestión Logística', 'Programa de Almacenamiento'),

        -- Gestión Comercial (1)
        ('Gestión Comercial', 'Gestión Comercial y Ventas'),

        -- Gestión de Producción (1)
        ('Gestión de Producción', 'Gestión de la Producción'),

        -- Gestión de Mantenimiento (3)
        ('Gestión de Mantenimiento', 'Calibración y Verificación de Equipos de Medición'),
        ('Gestión de Mantenimiento', 'Mantenimiento de Maquinaria y Equipos'),
        ('Gestión de Mantenimiento', 'Mantenimiento de Edificios e Instalaciones'),

        -- Gestión de Seguridad Física (1)
        ('Gestión de Seguridad Física', 'Seguridad y Vigilancia'),

        -- Gestión TICs (1)
        ('Gestión TICs', 'Tecnología Informática y de Comunicaciones'),

        -- Gestión Financiera (2)
        ('Gestión Financiera', 'Gestión Administrativa'),
        ('Gestión Financiera', 'Gestión Contable y Financiera'),

        -- Gestión de Compras (2)
        ('Gestión de Compras', 'Control de Proveedores'),
        ('Gestión de Compras', 'Gestión de Compras')
) AS v(area_nombre, subproceso_nombre)
INNER JOIN areas a
    ON flyway_normalizar_nombre_catalogo(a.nombre)
       = flyway_normalizar_nombre_catalogo(v.area_nombre)
WHERE NOT EXISTS (
    SELECT 1
    FROM subprogramas s
    WHERE s.area_id = a.id
      AND flyway_normalizar_nombre_catalogo(s.nombre)
          = flyway_normalizar_nombre_catalogo(v.subproceso_nombre)
);

-- ------------------------------------------------------------
-- 3. Tipos de documento (13 institucionales)
-- ------------------------------------------------------------
INSERT INTO tipos_documento (nombre, descripcion, activo)
SELECT v.nombre, NULL, TRUE
FROM (
    VALUES
        ('Manual'),
        ('Programa'),
        ('Proceso'),
        ('Procedimiento'),
        ('Política'),
        ('Reglamento'),
        ('Caracterización'),
        ('Instructivo'),
        ('Protocolo'),
        ('Formato'),
        ('Ficha Técnica'),
        ('Diagrama'),
        ('Otros Documentos')
) AS v(nombre)
WHERE NOT EXISTS (
    SELECT 1
    FROM tipos_documento t
    WHERE flyway_normalizar_nombre_catalogo(t.nombre)
          = flyway_normalizar_nombre_catalogo(v.nombre)
);

-- Eliminar únicamente la función auxiliar creada por esta migración.
DROP FUNCTION flyway_normalizar_nombre_catalogo(TEXT);
