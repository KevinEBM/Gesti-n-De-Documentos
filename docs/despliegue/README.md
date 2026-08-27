# Preparación para producción (A6) y despliegue (A7)

Este documento separa lo **preparado en el repositorio** (A6) de lo **pendiente de servidor real** (A7).

## A6 — Estado: preparado

El backend y el frontend pueden ejecutarse en un servidor si se configuran las variables de entorno correctas. **A6 no despliega** nada.

### Backend (JAR)

1. Compilar: `.\mvnw.cmd package` (desde `backend/`).
2. Exportar variables obligatorias (ver `backend/.env.example`).
3. Activar perfil producción: `SPRING_PROFILES_ACTIVE=prod`.
4. Ejecutar: `java -jar target/gestion-documental-0.0.1-SNAPSHOT.jar`.

**Variables obligatorias en producción:**

| Variable | Descripción |
|----------|-------------|
| `DB_URL` | JDBC PostgreSQL |
| `DB_USERNAME` | Usuario BD |
| `DB_PASSWORD` | Contraseña BD |
| `JWT_SECRET` | Secreto JWT Base64 (≥ 32 bytes decodificados) |
| `JWT_EXPIRATION_MS` | Expiración del token en milisegundos |
| `CORS_ALLOWED_ORIGINS` | Origen(es) del frontend (sin `*`; separar por coma si hay varios) |
| `STORAGE_LOCATION` | Ruta absoluta persistente para archivos (ej. `/var/lib/gestion-documental/uploads`) |
| `TZ` | Zona horaria del proceso. En producción **debe** ser `UTC` |

**Zona horaria (obligatorio en producción):** el proceso backend **debe** ejecutarse en UTC.

- Preferencia: `TZ=UTC`
- Alternativa si el entorno no permite `TZ`: `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC`

El módulo documental ya usa `Clock`/`UTC`. Todavía hay `LocalDateTime.now()` naive en catálogos, usuarios y `ApiResponse.fechaHora`. Fijar la timezone de la JVM evita depender de la zona del host. PostgreSQL y Flyway V1–V11 no necesitan cambios por esto.

**Perfil `prod`** (`application-prod.properties`):

- CORS y storage **sin** defaults de desarrollo.
- Logging menos verboso.
- Respuestas de error sin stack trace al cliente.
- `server.forward-headers-strategy=framework` para reverse proxy HTTPS (A7).

### Frontend (build estático)

1. Definir `VITE_API_BASE_URL` apuntando al backend accesible desde el navegador (ver `frontend/.env.example`).
2. `npm run build` → salida en `frontend/dist/`.
3. Servir `dist/` con reverse proxy o servidor estático. **No usar** `npm run dev` en producción.

Si frontend y API comparten dominio tras reverse proxy, en A7 puede evaluarse `VITE_API_BASE_URL` vacío o relativo; hoy el fallback de desarrollo es `http://localhost:8080`.

### Respaldo (obligatorio en producción)

Respaldar **juntos**:

- Base de datos PostgreSQL (metadatos, versiones, permisos).
- Directorio `STORAGE_LOCATION` (archivos físicos).

Respaldar solo la BD **deja el sistema inconsistente** si faltan archivos en disco.

### Puertos

- Backend: puede escuchar en **8080** internamente; no es necesario exponerlo a Internet.
- A7 decidirá reverse proxy (HTTPS) y exposición pública.

## A7 — Pendiente (servidor real)

No implementado en A6:

- Servidor / VM / hosting
- DNS y subdominio definitivo de Plantar
- Certificado HTTPS (Let's Encrypt u otro)
- Nginx u otro reverse proxy
- systemd / servicio del JAR
- PostgreSQL de producción
- Creación del directorio persistente de storage
- Valores reales de `CORS_ALLOWED_ORIGINS` y `VITE_API_BASE_URL`
- CI/CD, Docker, observabilidad

### Arquitectura esperada (referencia)

```text
Cliente
  ↓ HTTPS
Reverse proxy (Nginx, etc.)
  ↓
  ├── Frontend estático (dist/)
  └── /api → Spring Boot :8080
        ↓
      PostgreSQL + STORAGE_LOCATION (filesystem)
```

## Seguridad (sin cambios en A6)

- Autenticación JWT stateless; login público en `POST /api/auth/login`.
- Resto de endpoints autenticados según `@PreAuthorize`.
- No commitear `.env`, secretos JWT ni contraseñas reales.
- `backend/uploads/` ignorado por Git; en producción usar ruta externa configurable.

## Desarrollo local

Sigue funcionando sin perfil `prod`:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/gestion_documental"
$env:DB_USERNAME="..."
$env:DB_PASSWORD="..."
$env:JWT_SECRET="..."
$env:JWT_EXPIRATION_MS="3600000"
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
cd frontend
npm run dev
```
