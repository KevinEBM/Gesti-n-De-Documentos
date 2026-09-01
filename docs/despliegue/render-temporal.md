# Render — entorno temporal de pruebas

**No es el hosting definitivo de Plantar S.A.S.** Solo validación de despliegue Docker + PostgreSQL + frontend estático.

## Arquitectura de prueba

```text
Render Static Site (frontend/dist)
        ↓ HTTPS
Render Web Service (backend/Dockerfile, perfil prod)
        ↓
Render PostgreSQL (esquema vía Flyway V1–V5)
```

## Backend (Web Service)

- **Root directory / context:** `backend`
- **Dockerfile:** `backend/Dockerfile`
- **Perfil:** `SPRING_PROFILES_ACTIVE=prod`
- **Puerto:** Render inyecta `PORT`; Spring usa `server.port=${PORT:8080}`

### Variables obligatorias en Render (runtime, no en Git)

| Variable | Notas |
|----------|--------|
| `DB_URL` | JDBC interno de Render PostgreSQL |
| `DB_USERNAME` | Usuario Render |
| `DB_PASSWORD` | Contraseña Render |
| `JWT_SECRET` | Base64, ≥ 32 bytes decodificados |
| `JWT_EXPIRATION_MS` | Ej. `3600000` |
| `CORS_ALLOWED_ORIGINS` | URL del Static Site (sin `*`) |
| `STORAGE_LOCATION` | Ej. `/tmp/gestion-documental/uploads` |

### Storage efímero

El filesystem del Web Service **no es persistente**. Usar solo archivos ficticios de prueba. Tras redeploy/restart se pierden los binarios; la BD conserva metadatos.

## Frontend (Static Site)

Build con `VITE_API_BASE_URL` apuntando a la URL pública del Web Service backend.

## Local

Sin cambios: `PORT` ausente → puerto **8080**. Docker local opcional:

```bash
cd backend
docker build -t gestion-documental-render-test .
```

## Fases siguientes

- **R4:** crear servicios en Dashboard Render (manual, sin Blueprint)
- **R5:** smoke test end-to-end temporal
