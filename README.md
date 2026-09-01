# Gestión Documental Plantar S.A.S.

Aplicación web interna (intranet documental) para organizar, publicar, versionar y consultar documentos corporativos. Arquitectura **monolito multicapa**: backend Spring Boot + frontend React/Vite, integrados por API REST con JWT.

## Estado actual

El sistema está **funcional en desarrollo local** con backend real, frontend integrado, autenticación JWT, gestión documental completa (publicación, edición, estados, versiones, descargas), parametrización, usuarios y dashboard administrativo.

**Fuera de alcance actual:** módulo de notificaciones, envío de correo (SMTP), auditoría general, recuperación de contraseña por correo, despliegue a producción (ver sección final).

## Funcionalidades implementadas

| Área | Descripción |
|------|-------------|
| Autenticación | Login correo + contraseña, JWT, cierre de sesión, cambio voluntario de contraseña |
| Inicio | Datos reales del usuario (nombre, rol, área principal) |
| Dashboard | Solo ADMIN: métricas y actividad documental reciente desde API |
| Biblioteca | Listado, filtros, detalle y descarga de versión vigente según visibilidad |
| Gestión documental | Solo ADMIN: listado, edición, cambio de estado, nueva versión |
| Publicación | Solo ADMIN: documento nuevo con versión inicial configurable (entero ≥ 1; default formulario: 1) |
| Historial / descarga histórica | ADMIN y JEFE (visible); ADMINISTRATIVO sin acceso |
| Usuarios | CRUD administrativo, roles, área principal, contraseña inicial |
| Parametrización | Áreas, subprogramas y tipos de documento (activo/inactivo) |

## Roles

| Rol | Acceso principal |
|-----|------------------|
| **ADMINISTRADOR** | Todo lo anterior: dashboard, usuarios, parametrización, publicación, gestión, edición, estados, versiones, historial, descargas |
| **JEFE_AREA** | Inicio, biblioteca, detalle, descarga vigente, historial y descarga histórica en documentos **PUBLICADO** visibles (GLOBAL o área autorizada) |
| **ADMINISTRATIVO** | Inicio, biblioteca, detalle y descarga vigente en documentos **PUBLICADO** visibles; **sin** historial ni descarga histórica |

La autorización final la aplica el **backend** (`@PreAuthorize` + reglas de visibilidad). El frontend añade guards de UX.

## Tecnologías

| Capa | Stack |
|------|-------|
| Backend | Java **21**, Spring Boot **4.1.0**, Spring Security, JWT (jjwt 0.12.3), Spring Data JPA, Flyway, PostgreSQL |
| Frontend | **React 19**, **TypeScript 7**, **Vite 6**, TanStack Router, Tailwind CSS 4 |

Versiones tomadas de `backend/pom.xml` y `frontend/package.json`.

## Arquitectura

```text
gestion-documental/
├── backend/          # API REST, seguridad, persistencia, storage local
│   └── src/main/java/com/plantarsas/gestiondocumental/
│       ├── areas, auth, config, dashboard, documentos, exception,
│       ├── roles, security, shared, storage, subprogramas,
│       ├── tiposdocumento, usuarios
│       └── resources/db/migration/   # Flyway V1–V5
├── frontend/
│   └── src/app/      # UI React (rutas, componentes, lib/api)
└── docs/             # Documentación técnica
```

Detalle en [docs/arquitectura/README.md](docs/arquitectura/README.md).

## Base de datos

**9 tablas** de dominio (PostgreSQL, Flyway al arrancar):

`roles`, `areas`, `usuarios`, `usuario_area`, `subprogramas`, `tipos_documento`, `documentos`, `documento_area`, `versiones_documento`

Detalle en [docs/base-datos/README.md](docs/base-datos/README.md).

## Requisitos

- Java 21
- Maven (wrapper incluido en `backend/`)
- PostgreSQL
- Node.js compatible con Vite 6
- npm

## Configuración

### Base de datos

Crear base de datos, por ejemplo:

```text
gestion_documental
```

URL de ejemplo: `jdbc:postgresql://localhost:5432/gestion_documental`

Flyway aplica migraciones `V1`–`V5` al iniciar el backend.

### Variables de entorno (backend)

Propiedades reales en `backend/src/main/resources/application.properties`. **No subir secretos al repositorio.**

| Variable / propiedad | Obligatoria | Descripción |
|---------------------|-------------|-------------|
| `DB_URL` | Sí | JDBC PostgreSQL |
| `DB_USERNAME` | Sí | Usuario BD |
| `DB_PASSWORD` | Sí | Contraseña BD |
| `JWT_SECRET` | Sí | Secreto JWT (Base64, ≥ 32 bytes decodificados) → propiedad `jwt.secret` |
| `JWT_EXPIRATION_MS` | Sí | Expiración token en ms → propiedad `jwt.expiration-ms` |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes CORS (default `http://localhost:5173`) |
| `STORAGE_LOCATION` | No | Directorio de archivos (default `uploads`) |
| `STORAGE_MAX_FILE_SIZE` | No | Tamaño máximo bytes (default `10485760` = 10 MB) |
| `MAX_UPLOAD_FILE_SIZE` | No | Multipart Spring (default `10MB`) |
| `MAX_UPLOAD_REQUEST_SIZE` | No | Multipart request (default `11MB`) |

En desarrollo local los archivos suelen quedar en `backend/uploads/` si se ejecuta desde esa carpeta. Ver `backend/.env.example` (Spring Boot **no** carga `.env` automáticamente; exportar variables al entorno).

**Producción:** activar perfil `prod` con `SPRING_PROFILES_ACTIVE=prod`. Requiere `CORS_ALLOWED_ORIGINS` y `STORAGE_LOCATION` (ruta absoluta persistente). Detalle en [docs/despliegue/README.md](docs/despliegue/README.md).

### Variables de entorno (frontend)

Copiar `frontend/.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Ejecución local

**Backend:**

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Frontend:**

```powershell
cd frontend
npm install
npm run dev
```

Login en `http://localhost:5173/`. Tras autenticación, ADMIN va a panel administrativo; otros roles a inicio.

## Tests y build

**Backend:**

```powershell
cd backend
.\mvnw.cmd test          # 554 tests (1 skipped: contextLoads)
.\mvnw.cmd package       # genera JAR en target/ (no versionar)
.\mvnw.cmd compile
.\mvnw.cmd spring-boot:run
```

**Frontend:**

```powershell
cd frontend
npm run lint
npm run build
```

En Linux/macOS usar `./mvnw` en lugar de `.\mvnw.cmd`.

## Rutas frontend

| Ruta | Uso |
|------|-----|
| `/` | Login |
| `/app/inicio` | Bienvenida e información de cuenta |
| `/app/documentos` | Biblioteca |
| `/app/panel-admin` | Dashboard (ADMIN) |
| `/app/gestion-documentos` | Gestión (ADMIN) |
| `/app/publicar` | Publicación (ADMIN) |
| `/app/usuarios` | Usuarios (ADMIN) |
| `/app/parametrizacion` | Parametrización (ADMIN) |
| `/app/cambiar-contrasena` | Cambio de contraseña |
| `/app/documento/{id}` | Detalle |
| `/app/documento/{id}/editar` | Edición metadatos (ADMIN) |
| `/app/documento/{id}/actualizar` | Nueva versión (ADMIN) |
| `/app/documento/{id}/historial` | Historial (ADMIN / JEFE) |

No existe ruta `/app/notificaciones`.

## Seguridad (resumen)

- Autenticación stateless con JWT en cabecera `Authorization: Bearer …`
- `@PreAuthorize` en controladores; visibilidad documental por **estado PUBLICADO**, **alcance** (GLOBAL, AREA_RESPONSABLE, AREAS_ESPECIFICAS) y área del usuario
- `GET /api/roles` restringido a **ADMINISTRADOR**
- Archivos: máximo **10 MB**, extensión **.apk** bloqueada, hash **SHA-256** al guardar

## Preparación para producción (A6)

El proyecto está **preparado** para ejecutarse en servidor con variables de entorno y perfil `prod`. **No incluye despliegue** (servidor, DNS, HTTPS, Nginx → fase A7).

- Backend: JAR ejecutable + `application-prod.properties`
- Frontend: `npm run build` → `frontend/dist/` (servir estático; no `npm run dev`)
- Secretos: solo en entorno del servidor; ver `backend/.env.example` y `frontend/.env.example`
- Respaldo: PostgreSQL **y** directorio `STORAGE_LOCATION` juntos

Guía completa: [docs/despliegue/README.md](docs/despliegue/README.md).

## Despliegue — pendiente (A7)

La fase A7 contemplará servidor, variables de producción reales, HTTPS, subdominio, reverse proxy, proceso backend/frontend y storage persistente. **No está implementado** en este repositorio.

## Documentación adicional

- [docs/despliegue/README.md](docs/despliegue/README.md) — preparación A6 y pendientes A7
- [docs/arquitectura/README.md](docs/arquitectura/README.md) — módulos, endpoints, reglas documentales
- [docs/base-datos/README.md](docs/base-datos/README.md) — modelo de datos y migraciones
