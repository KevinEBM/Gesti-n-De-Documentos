-- ============================================================
-- V8: código institucional único para subprocesos (subprogramas).
--
-- El código documental tiene la forma:
--   CLASIFICACION-SUBPROCESO-TIPO-CONSECUTIVO   ej. PR-L&D-PG-01
-- donde el segundo segmento identifica al SUBPROCESO, no al área.
-- El área se deduce por la relación existente subprograma -> area.
--
-- Reglas:
--   - Los 40 nombres institucionales fueron verificados manualmente
--     en local y en Render: coinciden exactamente con los de V6,
--     sin faltantes, adicionales, duplicados ni espacios anómalos.
--     Por eso la asociación se hace por igualdad exacta de nombre.
--   - Migración autosuficiente: no usa flyway_normalizar_nombre_catalogo
--     (V6 la elimina al finalizar) ni extensiones como unaccent.
--   - No se persisten los alias históricos CD / LD. Los códigos
--     canónicos son C&D y L&D; el parser del frontend mantiene los
--     alias solo como compatibilidad de lectura.
--   - No se tocan documentos.codigo: cambiar el código de un
--     subproceso no reescribe códigos documentales ya publicados.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Columna inicialmente nullable para poder poblarla
-- ------------------------------------------------------------
ALTER TABLE subprogramas ADD COLUMN codigo VARCHAR(20);

-- ------------------------------------------------------------
-- 2. Asignación determinista por nombre institucional
-- ------------------------------------------------------------
UPDATE subprogramas s
SET codigo = v.codigo
FROM (
    VALUES
        ('Capacitación y Desarrollo',                         'C&D'),
        ('Gestión Humana',                                    'GHM'),
        ('Salud y Seguridad en el Trabajo',                   'SST'),
        ('Sustancias Químicas',                               'SQC'),
        ('Programa de Respeto, Convivencia e Inclusión',      'RCI'),
        ('Calidad del Agua Potable',                          'CAP'),
        ('Control de Plagas',                                 'CPL'),
        ('Control de Residuos Líquidos',                      'RLL'),
        ('Control de Residuos Sólidos',                       'CRS'),
        ('Limpieza y Desinfección',                           'L&D'),
        ('Gestión Ambiental',                                 'AMB'),
        ('Auditoría Interna',                                 'AUD'),
        ('Buenas Prácticas Higiénicas',                       'BPH'),
        ('Control de Alérgenos',                              'PCA'),
        ('Gestión Documental',                                'GDO'),
        ('Gestión de la Calidad',                             'GDC'),
        ('Material Extraño',                                  'MEX'),
        ('Plan de Muestreo',                                  'PDM'),
        ('Recall',                                            'REC'),
        ('Sistemas Integrados de Gestión',                    'SIG'),
        ('Trazabilidad',                                      'TZR'),
        ('Programa de Producto No Conforme',                  'PNC'),
        ('Desarrollo e Innovación',                           'DEI'),
        ('Proceso de Mejora Continua',                        'MCO'),
        ('Programa de Peticiones Quejas y Reclamos',          'PQR'),
        ('Almacén y Abastecimiento de Insumos',               'ABT'),
        ('Logística',                                         'LOG'),
        ('Transporte',                                        'TRS'),
        ('Programa de Almacenamiento',                        'ALM'),
        ('Gestión Comercial y Ventas',                        'GCV'),
        ('Gestión de la Producción',                          'GPR'),
        ('Calibración y Verificación de Equipos de Medición', 'CAL'),
        ('Mantenimiento de Maquinaria y Equipos',             'MME'),
        ('Mantenimiento de Edificios e Instalaciones',        'MEI'),
        ('Seguridad y Vigilancia',                            'SVG'),
        ('Tecnología Informática y de Comunicaciones',        'TIC'),
        ('Gestión Administrativa',                            'GAD'),
        ('Gestión Contable y Financiera',                     'GCF'),
        ('Control de Proveedores',                            'CPR'),
        ('Gestión de Compras',                                'GCO')
) AS v(nombre, codigo)
WHERE s.nombre = v.nombre;

-- ------------------------------------------------------------
-- 3. Guardas: no inventar códigos ni dejar unicidad ambigua
-- ------------------------------------------------------------
DO $$
DECLARE
    sin_codigo TEXT;
    codigos_repetidos TEXT;
BEGIN
    SELECT string_agg(format('id=%s nombre=%L', id, nombre), '; ' ORDER BY id)
    INTO sin_codigo
    FROM subprogramas
    WHERE codigo IS NULL;

    IF sin_codigo IS NOT NULL THEN
        RAISE EXCEPTION
            'V8 abortada: hay subprogramas sin codigo institucional asignado: %. Parametrice su codigo manualmente antes de reintentar.',
            sin_codigo;
    END IF;

    SELECT string_agg(codigo_normalizado, ', ' ORDER BY codigo_normalizado)
    INTO codigos_repetidos
    FROM (
        SELECT LOWER(codigo) AS codigo_normalizado
        FROM subprogramas
        GROUP BY LOWER(codigo)
        HAVING COUNT(*) > 1
    ) AS repetidos;

    IF codigos_repetidos IS NOT NULL THEN
        RAISE EXCEPTION
            'V8 abortada: los siguientes codigos quedaron asignados a mas de un subprograma: %.',
            codigos_repetidos;
    END IF;
END $$;

-- ------------------------------------------------------------
-- 4. El código pasa a ser obligatorio
-- ------------------------------------------------------------
ALTER TABLE subprogramas ALTER COLUMN codigo SET NOT NULL;

-- ------------------------------------------------------------
-- 5. Unicidad GLOBAL case-insensitive (no por área): el código
--    documental no incluye el área, así que un código debe
--    resolver un único subproceso.
-- ------------------------------------------------------------
CREATE UNIQUE INDEX uq_subprogramas_codigo_lower ON subprogramas (LOWER(codigo));
