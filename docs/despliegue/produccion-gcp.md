# Producción en Google Cloud (VM + Nginx en el host)

Runbook para cuando exista la VM. No sustituye secretos, dominio ni rutas reales: hay que reemplazar `${DOMINIO}` y `${FRONTEND_DIST}` al desplegar.

Este archivo **no** se aplica de una sola vez. El `infra/nginx/sig-plantar.conf` del repo incluye el bloque `:443` que apunta a certificados Let's Encrypt que **aún no existen** en un servidor nuevo. Si se carga el archivo completo antes de Certbot, Nginx no arranca (tampoco el bloque `:80`) y Certbot no puede validar el dominio.

## Arquitectura

```text
Internet
  ↓ :80 / :443
Nginx (host de la VM)
  ├── estáticos: ${FRONTEND_DIST}  (frontend/dist, build con pnpm fuera de Docker)
  └── /api/ → http://127.0.0.1:8080  (backend publicado solo en loopback)
                ↓ red Docker interna
              PostgreSQL 17 (servicio db, sin ports al host)
              volumen backend_uploads → /var/lib/gestion-documental/uploads
```

Nginx **no** va en Compose. El frontend **no** va en Compose.

## 1. VM, IP estática y firewall (GCP)

1. Crear la VM (Ubuntu LTS recomendado) y reservar una **IP externa estática**. Asociarla a la instancia.
2. DNS: registro **A** de `${DOMINIO}` → esa IP. Esperar a que resuelva antes de Certbot.
3. Firewall VPC (y `ufw` en la VM si se usa):
   - Permitir **22** (SSH), **80** (HTTP / ACME) y **443** (HTTPS) desde Internet.
   - **No** abrir **8080** ni **5432** al exterior. El backend solo escucha en `127.0.0.1:8080`; Postgres solo en la red Docker `interna`.

## 2. Software en la VM

Instalar Docker Engine, el plugin Compose, Nginx y Certbot (paquete que incluya el plugin Nginx, para que existan `options-ssl-nginx.conf` y `ssl-dhparams.pem`).

Clonar el repositorio en una ruta de trabajo (ejemplo: `/opt/gestion-documental`).

## 3. Secretos y Compose

1. Copiar `.env.production.example` → `.env.production` (está en `.gitignore`; no commitearlo).
2. Sustituir placeholders:
   - `DB_PASSWORD` real.
   - `JWT_SECRET`: generar con `openssl rand -base64 32` (debe decodificar a ≥ 32 bytes; no vale texto arbitrario).
   - `CORS_ALLOWED_ORIGINS=https://${DOMINIO}` (dominio real, no `documentos.example.com`).
3. No tocar: `DB_URL=jdbc:postgresql://db:5432/gestion_documental` (host `db`, base `gestion_documental`).
4. No tocar: `STORAGE_LOCATION=/var/lib/gestion-documental/uploads` (misma ruta del volumen `backend_uploads`).
5. No poner `SPRING_PROFILES_ACTIVE` en el `.env`: ya está en `docker-compose.yml`.
6. Desde la raíz del repo:

```bash
docker compose --env-file .env.production up -d
```

No hay endpoint de salud (`/api/health` no existe; Actuator no está en el `pom.xml`). Comprobar que el backend escucha en loopback contra una ruta real (`POST /api/auth/login` es `permitAll`):

```bash
curl -sS -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login
```

Se espera **400** o **405** (petición sin body o método GET). **000** o connection refused significa que el proceso no está escuchando. El puerto **no** debe ser alcanzable desde fuera de la VM.

## 4. Frontend estático

En la VM (o en CI, y luego copiar `dist/`):

```bash
cd frontend
pnpm install
VITE_API_BASE_URL=https://${DOMINIO} pnpm build
```

`VITE_API_BASE_URL` es obligatorio en el build: si falta, el cliente cae a `http://localhost:8080`. Debe ser el origen HTTPS público **sin** barra final (las rutas de la API ya empiezan por `/api`).

Dejar `frontend/dist/` en la ruta absoluta que se usará como `${FRONTEND_DIST}`.

## 5. Bootstrap de Nginx y certificado (orden obligatorio)

No instalar todavía el `.conf` completo del repo. Hacer **exactamente** estos pasos, en este orden.

### Paso 1 — Sustituir variables y dejar listo solo el bloque `:80`

Sustituir `${DOMINIO}` y `${FRONTEND_DIST}` (`envsubst` u otro método). Ejemplo:

```bash
export DOMINIO=documentos.example.com   # dominio real
export FRONTEND_DIST=/ruta/absoluta/frontend/dist
envsubst '${DOMINIO} ${FRONTEND_DIST}' \
  < infra/nginx/sig-plantar.conf \
  > /tmp/sig-plantar.sustituido.conf
```

Desplegar **temporalmente solo** el `server` de `:80` (redirect a HTTPS + `/.well-known/acme-challenge/`). **No** copiar aún el `server` de `:443`.

El archivo activo (p. ej. `/etc/nginx/sites-available/sig-plantar` enlazado en `sites-enabled`) debe contener únicamente, ya sustituido:

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name ${DOMINIO};

    client_max_body_size 11m;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}
```

(`server_name` ya con el dominio real, no el literal `${DOMINIO}`.)

### Paso 2 — Directorio ACME

```bash
sudo mkdir -p /var/www/certbot
```

### Paso 3 — Arrancar Nginx solo con el bloque `:80`

```bash
sudo nginx -t
sudo systemctl enable --now nginx
# si Nginx ya corría: sudo systemctl reload nginx
```

En este momento el redirect 301 a HTTPS es esperado; Certbot usa solo `/.well-known/acme-challenge/`, que no redirige.

### Paso 4 — Emitir el certificado

```bash
sudo certbot certonly --webroot -w /var/www/certbot -d ${DOMINIO}
```

No usar `certbot --nginx` en este bootstrap: el bloque `:443` del repo aún no está activo y los PEM no existen.

### Paso 5 — Agregar el bloque `:443` cuando el certificado exista

Comprobar:

```text
/etc/letsencrypt/live/${DOMINIO}/fullchain.pem
/etc/letsencrypt/live/${DOMINIO}/privkey.pem
```

También deben existir (los instala el paquete Certbot/Nginx; el `.conf` del repo los `include`):

```text
/etc/letsencrypt/options-ssl-nginx.conf
/etc/letsencrypt/ssl-dhparams.pem
```

Si faltan, instalar el plugin Nginx de Certbot **antes** de pegar el bloque `:443`. Sin esos archivos `nginx -t` falla igual que sin el certificado.

Cuando existan, agregar al mismo archivo el `server` de `:443` ya sustituido (el del repo: `root`, `/api/` → `http://127.0.0.1:8080`, encabezados `Host` / `X-Real-IP` / `X-Forwarded-For` / `X-Forwarded-Proto`, `client_max_body_size 11m`).

### Paso 6 — Sintaxis y recarga

```bash
sudo nginx -t
sudo systemctl reload nginx
```

### Paso 7 — Renovación automática

```bash
sudo systemctl status certbot.timer
# si el timer no existe en esa distro:
sudo systemctl list-timers '*certbot*'
# prueba en seco:
sudo certbot renew --dry-run
```

Sin timer/cron equivalente, el certificado caduca ~90 días y hay que repetir el bootstrap a mano. Dejar el timer **enabled** y activo.

## 6. Comprobaciones

- `https://${DOMINIO}` sirve el SPA; recargar una ruta interna (p. ej. `/login`) no debe dar 404 ( `try_files` → `index.html`).
- `https://${DOMINIO}/api/...` llega al backend (no a estáticos).
- Subida de archivos: Nginx `11m` = `MAX_UPLOAD_REQUEST_SIZE` default (`11MB` en `application.properties`).
- Desde fuera de la VM, `8080` y `5432` no responden.

## 7. Respaldo

Respaldar **juntos** el volumen de PostgreSQL (`postgres_data`) y el de archivos (`backend_uploads` → `STORAGE_LOCATION`). Solo la BD deja el sistema inconsistente.

## Referencias en el repo

| Archivo | Uso |
|---------|-----|
| `docker-compose.yml` | `db` + `backend`; `SPRING_PROFILES_ACTIVE=prod` |
| `.env.production.example` | plantilla de secretos (copiar a `.env.production`) |
| `infra/nginx/sig-plantar.conf` | plantilla completa; **no** aplicarla de una vez en servidor nuevo |
| `docs/despliegue/README.md` | variables, UTC, encabezados al backend |
