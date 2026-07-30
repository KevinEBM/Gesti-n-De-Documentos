# Arquitectura

Este directorio contiene documentacion de arquitectura del Sistema Interno de Gestion Documental.

La organizacion acordada separa `backend/` y `frontend/`. El backend usa el paquete base `com.plantarsas.gestiondocumental` y debe organizarse por funcionalidades, no por capas globales. Los modulos previstos son `areas`, `auditoria`, `auth`, `categorias`, `config`, `correo`, `documentos`, `exception`, `notificaciones`, `roles`, `security`, `shared`, `storage`, `tiposdocumento`, `usuarios` y `versiones`.

No hay proveedor definitivo de infraestructura, correo ni almacenamiento.
