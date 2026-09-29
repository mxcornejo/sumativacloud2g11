# Evidencia verificada · 29 septiembre 2026

## Resultados comprobados
- 14 pruebas locales aprobadas sin fallos: seis de contrato EDA, cuatro de esquemas GraphQL y cuatro del BFF.
- Seis funciones Java 17 compiladas y desplegadas en sumativa1usuariosrolesmc2026: UsuariosFunction, RolesFunction, UsuariosGraphQLFunction, RolesGraphQLFunction, PublicarEventos y AuditarEvento.
- Topic sumativa3-usuarios-roles-g11 creado en Brazil South; suscripción auditoria-usuarios-roles conectada al consumidor y filtrada por los seis tipos CRUD.
- Migración aditiva Oracle aplicada; triggers compilados sin errores.
- ROLLBACK_ATOMICITY_OK: una operación de prueba y su evento se revirtieron juntos.
- BFF EC2 http://44.210.30.141: health, roles y users respondieron HTTP 200. Las consultas de negocio comprobaron conexión con Azure y Oracle.
- Circuito completo por BFF: 18 comprobaciones HTTP correctas, incluyendo CRUD REST, CRUD GraphQL sin errors, 409 por rol en uso y 404 por usuario eliminado.
- 12 eventos del circuito anterior publicados y procesados: creación, modificación y eliminación de dos roles y dos usuarios. Correlación por event_id en evidencia-eventos.json.
- Los registros de prueba de ese circuito se eliminaron; se conservaron sus eventos como evidencia.

- IDEMPOTENCY_ORACLE_OK: ejecución local del consumidor dos veces con el mismo evento persistido; Oracle conservó una sola fila. Esta prueba usa Oracle real y no simula una reentrega de Event Grid.

## Evidencia trazable
La ejecución completa terminó el 29 de septiembre de 2026 a las 13:14:36 UTC. Subjects: /roles/44, /usuarios/42, /roles/45 y /usuarios/43. evidencia-http.json conserva estados HTTP; evidencia-eventos.json conserva los 12 IDs y sus estados de publicación y consumo. No contienen credenciales ni datos personales.

Una prueba previa directa a Azure tuvo una interrupción de conexión durante GraphQL. Se limpiaron los registros identificados y se completó después la prueba por EC2. No se cuenta esa primera ejecución incompleta como prueba aprobada.

## Pendiente de entrega académica
- Video Kaltura de ambos integrantes, de 4 a 8 minutos, y enlace real con permisos para el docente.
- Evidencia de participación equitativa en Git: no se pueden atribuir artificialmente commits a la pareja.
- Confirmar el cierre efectivo en AVA y subir allí el paquete final.

## Límites operativos
No se configuró dead-letter ni alertas. No se acredita recuperación ante agotamiento de reintentos, carga masiva ni orden de entrega. La prueba de la plataforma es académica; no representa una certificación de producción.
