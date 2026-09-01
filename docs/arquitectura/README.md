# Arquitectura

Documentación de la arquitectura actual del **Sistema de Gestión Documental Plantar S.A.S.**

## Visión general

Monolito multicapa:

- **Backend:** API REST JSON, Spring Boot 4.1.0, Java 21, PostgreSQL, Flyway, almacenamiento local de archivos.
- **Frontend:** SPA React 19 + TypeScript, Vite 6, TanStack Router; consume la API con JWT almacenado en `sessionStorage`.

El backend es la **autoridad** de seguridad y reglas de negocio. El frontend aplica guards de UX que no sustituyen la validación server-side.

## Módulos backend (implementados)

Paquete base: `com.plantarsas.gestiondocumental`

| Módulo | Responsabilidad |
|--------|-----------------|
| `auth` | Login, cambio de contraseña |
| `security` | JWT, filtros, `UsuarioAreaAutorizacionService` |
| `usuarios` | CRUD usuarios, área principal, estados ACTIVO/INACTIVO |
| `roles` | Catálogo de roles (solo consulta ADMIN) |
| `areas` | Parametrización de áreas |
| `subprogramas` | Subprogramas (pertenecen a una sola área) |
| `tiposdocumento` | Tipos de documento parametrizables |
| `documentos` | Publicación, consulta, edición, estados, versiones, descargas |
| `dashboard` | Métricas y actividad reciente (ADMIN) |
| `storage` | Persistencia local de archivos |
| `shared` | DTOs, enums (`DocumentoEstado`, `DocumentoAlcance`, `RolEnum`) |
| `config` | Seguridad, CORS, reloj |
| `exception` | Manejo centralizado de errores API |

**No implementados como módulos funcionales:** `notificaciones`, `correo`/SMTP, `auditoria` general.

## Frontend

Código activo en `frontend/src/app/`:

- `routes/` — pantallas (TanStack Router file-based)
- `components/` — UI (AppShell, badges, formularios)
- `lib/` — cliente API, store de sesión, helpers

Alias `@` → `src/app` (ver `vite.config.ts`).

## Autenticación y sesión

1. `POST /api/auth/login` con correo y contraseña.
2. Respuesta incluye JWT, rol y área principal (`areaPrincipalId`, `areaPrincipalNombre`).
3. Frontend guarda sesión en `sessionStorage`.
4. `PUT /api/auth/contrasena` — cualquier usuario autenticado; requiere contraseña actual; nueva ≥ 8 caracteres; JWT vigente se mantiene.

**No implementado:** recuperación por correo, contraseña temporal, cambio obligatorio en primer ingreso, SMTP.

Al crear usuarios, el ADMIN define **contraseña inicial** (no “contraseña temporal”).

## Gestión documental

### Publicación inicial (solo ADMIN)

- Código manual (único, case-insensitive, trim).
- Título, descripción, área responsable, subprograma, tipo, alcance, áreas adicionales si aplica.
- **Versión inicial** (`numeroVersionInicial`): entero obligatorio ≥ 1; la indica el ADMINISTRADOR según la versión actual del documento fuera del sistema. En el formulario **Publicar documento** el campo es editable, requerido y arranca en **1** por defecto.
- Archivo + descripción de versión inicial (multipart `metadata` incluye `numeroVersionInicial`).
- Resultado: documento **PUBLICADO**, versión registrada con el número indicado (ej. **7**), `vigente = true`.

La versión inicial **solo** se define en esta publicación; no se puede cambiar desde edición de metadatos.

### Alcance (`DocumentoAlcance`)

| Valor | Significado |
|-------|-------------|
| `GLOBAL` | Visible para todos los roles no admin (si PUBLICADO) |
| `AREA_RESPONSABLE` | Visible según área responsable del documento |
| `AREAS_ESPECIFICAS` | Visible según áreas autorizadas en `documento_area` |

Usuarios no ADMIN solo ven documentos en estado **PUBLICADO** y según alcance/área.

### Estados (`DocumentoEstado`)

| Estado | Transiciones permitidas |
|--------|-------------------------|
| PUBLICADO | → INACTIVO, OBSOLETO |
| INACTIVO | → PUBLICADO, OBSOLETO |
| OBSOLETO | → PUBLICADO |

**No permitido:** OBSOLETO → INACTIVO.

Cambio de estado **no** crea versión ni cambia archivo.

**OBSOLETO:** consulta/descarga/historial/reactivación sí (ADMIN); edición y nueva versión **no**.

**INACTIVO:** permite nueva versión; sigue INACTIVO tras publicar versión.

### Versionamiento

- La publicación inicial registra la **versión actual indicada por el ADMINISTRADOR** (ej. 1, 3, 7, 15). No se crean versiones ficticias anteriores.
- Las **nuevas versiones** (POST `/api/documentos/{id}/versiones`) **no** permiten elegir el número manualmente: el sistema calcula `última versión + 1` (ej. 7 → 8 → 9).
- Una sola versión **vigente** por documento.
- Cada versión: archivo, fecha, descripción del cambio, publicador.
- Editar metadatos del documento **no** crea versión ni modifica el número de versión.
- Cambiar código en edición **no** crea versión.

**Compatibilidad:** documentos registrados antes de este comportamiento conservan su numeración (ej. 1 → 2). Solo las publicaciones nuevas pueden iniciar en otro número.

### Historial (ejemplo versión inicial 7)

Carga inicial al sistema (versión indicada: 7):

```text
Versión 7 [Vigente]
```

Tras una actualización (automática → 8):

```text
Versión 8 [Vigente]
Versión 7
```

No aparecen versiones 1–6 porque nunca se almacenaron en el sistema.

### Historial y descargas

| Acción | ADMIN | JEFE | ADMINISTRATIVO |
|--------|-------|------|----------------|
| Descarga vigente | Sí* | Sí* | Sí* |
| Listar versiones | Sí* | Sí* | No |
| Descarga histórica (`versionId`) | Sí* | Sí* | No |

\*Según visibilidad del documento.

Descarga histórica **no** restaura versión, no cambia vigente ni estado.

### Archivos

- Máximo **10 MB** (10 485 760 bytes).
- Extensión **.apk** rechazada.
- Almacenamiento local configurable (`STORAGE_LOCATION`, default `uploads`).
- SHA-256 calculado al guardar; protección path traversal en `StorageServiceImpl`.

## Dashboard (solo ADMIN)

Endpoints:

- `GET /api/dashboard` — documentos publicados, usuarios activos, áreas registradas.
- `GET /api/dashboard/actividad-reciente` — publicaciones y nuevas versiones recientes.

No hay métricas en tablas dedicadas ni usuarios conectados en tiempo real.

## Inicio

Pantalla simple: nombre, rol, área (o “No aplica” para ADMIN sin área). Sin documentos recientes, notificaciones ni mocks.

## API — resumen de endpoints

Roles indicados son los exigidos por `@PreAuthorize` (usuario autenticado salvo login).

### Auth

| Método | Ruta | Rol |
|--------|------|-----|
| POST | `/api/auth/login` | Anónimo |
| PUT | `/api/auth/contrasena` | Autenticado |

### Dashboard

| Método | Ruta | Rol |
|--------|------|-----|
| GET | `/api/dashboard` | ADMINISTRADOR |
| GET | `/api/dashboard/actividad-reciente` | ADMINISTRADOR |

### Usuarios

| Método | Ruta | Rol |
|--------|------|-----|
| POST | `/api/usuarios` | ADMINISTRADOR |
| GET | `/api/usuarios` | ADMINISTRADOR |
| GET | `/api/usuarios/{id}` | ADMINISTRADOR |
| PUT | `/api/usuarios/{id}` | ADMINISTRADOR |
| PATCH | `/api/usuarios/{id}/estado` | ADMINISTRADOR |

### Roles

| Método | Ruta | Rol |
|--------|------|-----|
| GET | `/api/roles` | ADMINISTRADOR |
| GET | `/api/roles/{id}` | ADMINISTRADOR |

### Áreas / subprogramas / tipos

CRUD de escritura: **ADMINISTRADOR**. Listados de consulta también accesibles a JEFE y ADMINISTRATIVO donde aplique (catálogos activos).

Rutas base: `/api/areas`, `/api/subprogramas`, `/api/tipos-documento`.

### Documentos

| Método | Ruta | Rol |
|--------|------|-----|
| POST | `/api/documentos` | ADMINISTRADOR |
| GET | `/api/documentos` | ADMIN, JEFE, ADMINISTRATIVO |
| GET | `/api/documentos/{id}` | ADMIN, JEFE, ADMINISTRATIVO |
| PUT | `/api/documentos/{id}` | ADMINISTRADOR |
| PATCH | `/api/documentos/{id}/estado` | ADMINISTRADOR |
| GET | `/api/documentos/{id}/descarga` | ADMIN, JEFE, ADMINISTRATIVO |
| POST | `/api/documentos/{id}/versiones` | ADMINISTRADOR |
| GET | `/api/documentos/{id}/versiones` | ADMIN, JEFE |
| GET | `/api/documentos/{id}/versiones/{versionId}/descarga` | ADMIN, JEFE |

## Fuera de alcance / evolución futura

Posibles extensiones **no implementadas**:

- Centro de notificaciones persistente
- Envío de correo (SMTP) al publicar
- Auditoría transversal en tabla dedicada
- Recuperación de contraseña por email
- Despliegue cloud / CDN / object storage remoto

La **actividad reciente** del dashboard no sustituye un módulo de notificaciones.
