# Catálogos institucionales

Referencia para la precarga Flyway (V6) y para la futura extracción automática de metadatos desde el nombre de archivo (M4).

**Fuente de verdad en runtime:** PostgreSQL → Backend API → Frontend.
Este documento **no** sustituye la BD; conserva nomenclatura y abreviaturas para el parser del frontend.

Los datos ficticios del frontend externo (`data.ts`) **no** se importan. Las descripciones de áreas de esa fuente estaban cruzadas; V6 inserta `descripcion = NULL`.

---

## 1. Áreas (11)

Códigos **técnicos internos del sistema** (`areas.codigo`). **No** son abreviaturas de la nomenclatura documental (`PR`, `IN`, `SST`, etc.).

| Código | Nombre |
|--------|--------|
| GHUM | Gestión Humana |
| GAMB | Gestión Ambiental |
| GCAL | Gestión de Calidad |
| GLOG | Gestión Logística |
| GCOM | Gestión Comercial |
| GPROD | Gestión de Producción |
| GMANT | Gestión de Mantenimiento |
| GSEGF | Gestión de Seguridad Física |
| GTICS | Gestión TICs |
| GFIN | Gestión Financiera |
| GCOMP | Gestión de Compras |

---

## 2. Subprocesos (40)

Entidad interna: tabla `subprogramas`. Visualmente en UI: **Subproceso / Subprocesos**.

### Gestión Humana (5)

| Abrev. | Aliases | Nombre canónico |
|--------|---------|-----------------|
| C&D | CD | Capacitación y Desarrollo |
| GHM | — | Gestión Humana |
| SST | — | Salud y Seguridad en el Trabajo |
| SQC | — | Sustancias Químicas |
| RCI | — | Programa de Respeto, Convivencia e Inclusión |

### Gestión Ambiental (6)

| Abrev. | Aliases | Nombre canónico |
|--------|---------|-----------------|
| CAP | — | Calidad del Agua Potable |
| CPL | — | Control de Plagas |
| RLL | — | Control de Residuos Líquidos |
| CRS | — | Control de Residuos Sólidos |
| L&D | LD | Limpieza y Desinfección |
| AMB | — | Gestión Ambiental |

### Gestión de Calidad (14)

| Abrev. | Nombre canónico |
|--------|-----------------|
| AUD | Auditoría Interna |
| BPH | Buenas Prácticas Higiénicas |
| PCA | Control de Alérgenos |
| GDO | Gestión Documental |
| GDC | Gestión de la Calidad |
| MEX | Material Extraño |
| PDM | Plan de Muestreo |
| REC | Recall |
| SIG | Sistemas Integrados de Gestión |
| TZR | Trazabilidad |
| PNC | Programa de Producto No Conforme |
| DEI | Desarrollo e Innovación |
| MCO | Proceso de Mejora Continua |
| PQR | Programa de Peticiones Quejas y Reclamos |

### Gestión Logística (4)

| Abrev. | Nombre canónico |
|--------|-----------------|
| ABT | Almacén y Abastecimiento de Insumos |
| LOG | Logística |
| TRS | Transporte |
| ALM | Programa de Almacenamiento |

### Gestión Comercial (1)

| Abrev. | Nombre canónico |
|--------|-----------------|
| GCV | Gestión Comercial y Ventas |

### Gestión de Producción (1)

| Abrev. | Nombre canónico |
|--------|-----------------|
| GPR | Gestión de la Producción |

### Gestión de Mantenimiento (3)

| Abrev. | Nombre canónico |
|--------|-----------------|
| CAL | Calibración y Verificación de Equipos de Medición |
| MME | Mantenimiento de Maquinaria y Equipos |
| MEI | Mantenimiento de Edificios e Instalaciones |

### Gestión de Seguridad Física (1)

| Abrev. | Nombre canónico |
|--------|-----------------|
| SVG | Seguridad y Vigilancia |

### Gestión TICs (1)

| Abrev. | Nombre canónico |
|--------|-----------------|
| TIC | Tecnología Informática y de Comunicaciones |

### Gestión Financiera (2)

| Abrev. | Nombre canónico |
|--------|-----------------|
| GAD | Gestión Administrativa |
| GCF | Gestión Contable y Financiera |

### Gestión de Compras (2)

| Abrev. | Nombre canónico |
|--------|-----------------|
| CPR | Control de Proveedores |
| GCO | Gestión de Compras |

**Total: 40 subprocesos.**

Las abreviaturas **no** se almacenan en PostgreSQL en V6 (no hay columna dedicada). Se usarán en M4 en el parser del frontend.

---

## 3. Tipos de documento (13)

| Abrev. | Nombre canónico |
|--------|-----------------|
| MA | Manual |
| PG | Programa |
| PC | Proceso |
| PD | Procedimiento |
| PL | Política |
| RT | Reglamento |
| CR | Caracterización |
| IN | Instructivo |
| PT | Protocolo |
| FO | Formato |
| FT | Ficha Técnica |
| DG | Diagrama |
| OD | Otros Documentos |

**Normalización:** la fuente externa usaba `Politica` sin tilde en algunos mapas; el nombre canónico en BD es **Política**.

Las abreviaturas **no** se almacenan en PostgreSQL en V6.

---

## 4. Formato del código documental (M4)

```text
CLASIFICACIÓN-SUBPROCESO-TIPO-CONSECUTIVO
```

Ejemplo:

```text
PR-L&D-IN-03 Nombre del documento V7.pdf
```

| Segmento | Ejemplo | Significado |
|----------|---------|-------------|
| Clasificación | PR | Campo separado; **no** es código de área |
| Subproceso | L&D | Abreviatura del subproceso |
| Tipo | IN | Abreviatura del tipo |
| Consecutivo | 03 | Número correlativo |

**Área:** se deduce del subproceso. Ejemplo: `L&D` → Limpieza y Desinfección → Gestión Ambiental.

No existe campo `clasificacion` en el backend actual. M4 puede usarlo solo durante el parseo.

---

## 5. Versiones en nombre de archivo (M4)

Patrones reconocibles al final del nombre:

```text
V1, V1.0, V2, V2.1, V7
```

Backend actual: `numeroVersionInicial` (entero ≥ 1).

| Patrón | Compatibilidad propuesta |
|--------|--------------------------|
| V1, V7 | → 1, 7 |
| V1.0 | → 1 (parte entera) |
| V2.1 | **No convertir silenciosamente**; M4 debe definir regla (inválido o entero con aviso) |

---

## 6. Elementos excluidos

| Elemento | Motivo |
|----------|--------|
| **Plantilla** | No pertenece a los 13 tipos institucionales; puede existir solo como icono visual en frontend |
| **Maquinaria y Equipo** | No pertenece a los 40 subprocesos institucionales; puede existir solo como icono visual |
| IDs ficticios `a1`, `c1`, `t1` | No usar; PostgreSQL genera BIGINT |
| Usuarios/documentos demo | No importar |

---

## 7. Migración Flyway V6

Archivo: `backend/src/main/resources/db/migration/V6__cargar_catalogos_institucionales.sql`

Comportamiento:

- Insert idempotente por **nombre normalizado** (minúsculas, sin tildes, espacios colapsados).
- `descripcion = NULL`, `activo = TRUE` solo en registros nuevos.
- Colisión de `areas.codigo` con nombre distinto → migración aborta.
- Área existente con mismo nombre normalizado → se reutiliza; no se actualiza su código.
- Subprograma/tipo duplicado (misma clave lógica) → no se inserta ni se reactiva.

---

## 8. Resolución futura en frontend (M4)

El extractor debería devolver nombres/abreviaturas, no IDs ficticios:

```ts
{
  codigoDocumento: "PR-L&D-IN-03",
  nombreDocumento: "...",
  numeroVersionInicial?: number,
  clasificacion: "PR",
  subproceso: { abreviatura: "L&D", nombre: "Limpieza y Desinfección", areaNombre: "Gestión Ambiental" },
  tipoDocumento: { abreviatura: "IN", nombre: "Instructivo" }
}
```

Resolución contra catálogos cargados vía API:

```text
normalizarNombre(nombre) → registro en listarSubprogramas / listarTiposDocumento → id BIGINT real
```

El `areaId` debe obtenerse de la relación del subproceso retornado por el backend.
