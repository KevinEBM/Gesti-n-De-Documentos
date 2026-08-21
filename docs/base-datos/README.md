# Base de datos

Modelo de datos actual del **Sistema de Gestión Documental Plantar S.A.S.** (PostgreSQL, Flyway).

## Tablas de dominio (9)

| Tabla | Finalidad |
|-------|-----------|
| `roles` | Catálogo de roles del sistema (ADMINISTRADOR, JEFE_AREA, ADMINISTRATIVO) |
| `areas` | Áreas organizacionales parametrizables |
| `usuarios` | Cuentas de acceso (correo, contraseña BCrypt, rol, estado ACTIVO/INACTIVO) |
| `usuario_area` | Relación usuario–área; una **área principal** por usuario operativo |
| `subprogramas` | Subprogramas; cada uno pertenece a **una sola área** |
| `tipos_documento` | Tipos de documento parametrizables |
| `documentos` | Identidad del documento: código, título, estado, alcance, área responsable, etc. |
| `documento_area` | Áreas autorizadas adicionales y área responsable según alcance |
| `versiones_documento` | Versiones numeradas del documento (entero ≥ 1; único por documento); archivo, vigencia, descripción del cambio, publicador |

Relación principal: **Documento 1 — N VersionDocumento** (una sola versión vigente a la vez).

El **código documental** vive en `documentos.codigo`: asignado en publicación inicial, único (case-insensitive), editable sin crear nueva versión, se conserva entre versiones.

### Columna `versiones_documento.numero_version`

- Tipo **INTEGER**, obligatorio.
- Unicidad por documento: constraint `uq_versiones_documento_numero` (`documento_id`, `numero_version`).
- La versión inicial configurable (publicación) usa este campo con el valor indicado por el ADMINISTRADOR; no requiere migración adicional.
- Las versiones posteriores incrementan automáticamente desde el máximo existente (`MAX(numero_version) + 1`).

## Estados y enums

**Documento:** `PUBLICADO`, `INACTIVO`, `OBSOLETO` (ver transiciones en [arquitectura](../arquitectura/README.md)).

**Alcance documento:** `GLOBAL`, `AREA_RESPONSABLE`, `AREAS_ESPECIFICAS`.

**Usuario:** `ACTIVO`, `INACTIVO`.

## Migraciones Flyway

Ubicación: `backend/src/main/resources/db/migration/`

| Versión | Contenido relevante |
|---------|---------------------|
| V1 | Usuarios, roles, áreas, `usuario_area` |
| V2 | Documentos, tipos, versiones, `documento_area`; elimina concepto password temporal |
| V3 | **Renombra** `categorias` → `subprogramas` (histórico; el modelo actual usa subprogramas) |
| V4 | Ajuste modelo área–documento (una fila principal en `documento_area`) |
| V5 | Columna `alcance` en documentos |

Flyway se ejecuta al arrancar el backend (`spring.flyway.enabled=true`). Hibernate valida el esquema (`ddl-auto=validate`).

## Lo que NO existe en el esquema actual

No hay tablas funcionales para:

- `notificaciones`
- `codigos_verificacion`
- `auditoria`
- `envios_correo`

Si documentación antigua menciona 10–12 tablas o “categorías” como entidad actual, está **obsoleta**. El concepto funcional vigente es **subprograma**.

## Configuración local

1. Crear base PostgreSQL, por ejemplo `gestion_documental`.
2. Configurar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
3. Iniciar backend; Flyway crea/actualiza el esquema.

Ejemplo URL (sin credenciales):

```text
jdbc:postgresql://localhost:5432/gestion_documental
```

## Notas históricas

Las migraciones V1–V3 pueden contener nombres antiguos (`categorias`, `password_temporal`) como registro del evolución del esquema. **No deben interpretarse como el modelo funcional actual.**
