# Sistema Interno de Gestion Documental

Sistema interno para organizar, publicar, versionar y consultar documentos corporativos de Plantar S.A.S. El repositorio separa el backend Spring Boot del frontend React/Vite para que ambos puedan evolucionar de forma independiente.

Proveedor de infraestructura y almacenamiento definitivo por definir.

## Alcance

El sistema contempla gestion de usuarios, roles, areas, categorias, tipos de documento, documentos, versiones, notificaciones, codigos de verificacion, auditoria, autenticacion y correo. Esta refactorizacion solo organiza la estructura base; no implementa flujos de negocio completos ni define proveedor de nube.

## Roles

- ADMINISTRADOR
- JEFE_AREA
- ADMINISTRATIVO

## Estructura del repositorio

```text
backend/
  .mvn/
  mvnw
  mvnw.cmd
  pom.xml
  src/main/java/com/plantarsas/gestiondocumental/
  src/main/resources/
  src/test/java/com/plantarsas/gestiondocumental/
frontend/
  public/
  src/app/App.jsx
  src/main.jsx
  package.json
  vite.config.js
docs/
  arquitectura/
  base-datos/
```

## Backend

Requisitos:

- Java 21
- Maven Wrapper incluido en `backend/`
- PostgreSQL para el desarrollo de persistencia cuando el modelo este definido

Ejecutar:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Validar:

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

## Frontend

Requisitos:

- Node.js compatible con Vite 6
- npm

Ejecutar:

```powershell
cd frontend
npm install
npm run dev
```

Validar:

```powershell
cd frontend
npm run lint
npm run build
```

## Variables de entorno

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `STORAGE_LOCATION`

No se deben subir credenciales, tokens, documentos reales ni configuraciones privadas. La ruta local de almacenamiento se configura con `storage.location=${STORAGE_LOCATION:uploads}`.

## Modulos implementados

- Aplicacion Spring Boot base: `GestionDocumentalApplication`.
- Prueba de carga de contexto Spring Boot.
- Contratos genericos iniciales para `storage` y `correo`.
- Frontend base React/Vite con `App.jsx` ubicado en `frontend/src/app`.

## Modulos pendientes

Los siguientes modulos estan previstos pero aun no tienen implementacion funcional completa:

- `areas`
- `auditoria`
- `auth`, incluyendo login, JWT, cambio de contrasena, contrasena temporal, recuperacion, codigos de verificacion, expiracion e intentos fallidos. Las contrasenas deberan protegerse con `PasswordEncoder`.
- `categorias`
- `documentos`
- `notificaciones`
- `roles`
- `security`
- `tiposdocumento`
- `usuarios`
- `versiones`

La base de datos contempla doce tablas: `roles`, `usuarios`, `areas`, `usuario_area`, `categorias`, `tipos_documento`, `documentos`, `documento_area`, `versiones_documento`, `notificaciones`, `codigos_verificacion` y `auditoria`. No se contempla una tabla `envios_correo`.

El codigo documental pertenece a `documentos.codigo`: lo asigna el administrador en la publicacion inicial, es obligatorio, unico, sirve para buscar documentos, se conserva en todas las versiones y no debe duplicarse en `versiones_documento`.

La identidad general del documento pertenece al modulo `documentos`; los archivos historicos pertenecen al modulo `versiones`. La relacion conceptual es `Documento 1 - N VersionesDocumento`.

## Almacenamiento

El almacenamiento debe permanecer desacoplado de proveedores especificos. El contrato base esta en `backend/src/main/java/com/plantarsas/gestiondocumental/storage/StorageService.java`. No hay integracion real con Google Cloud, AWS, Azure u otro proveedor en esta etapa.

## Documentacion

La documentacion tecnica vive en `docs/`. Las migraciones SQL de Flyway, cuando exista el modelo completo, deberan ubicarse en `backend/src/main/resources/db/migration`.
