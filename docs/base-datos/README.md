# Base de Datos

Este directorio contiene documentacion del modelo de datos.

Tablas previstas:

- `roles`
- `usuarios`
- `areas`
- `usuario_area`
- `categorias`
- `tipos_documento`
- `documentos`
- `documento_area`
- `versiones_documento`
- `notificaciones`
- `codigos_verificacion`
- `auditoria`

No se debe crear la tabla `envios_correo`.

Las migraciones Flyway estan pendientes hasta contar con el modelo SQL completo. Cuando existan, deben ubicarse en `backend/src/main/resources/db/migration`.
