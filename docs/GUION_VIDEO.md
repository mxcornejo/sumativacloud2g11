# Guion de demostración · Kaltura · 7 minutos

Ensayar con ambos integrantes, letra legible y sin mostrar claves, local.settings.json ni wallet. Preparar Azure, el diagrama, Postman, el código y una consulta de auditoría. Sustituir cada resultado esperado por lo observado; si falla, no simular éxito.

| Tiempo | Participante | Explicación y demostración |
|---|---|---|
| 0:00–0:45 | Manuel | Presentar Usuarios/Roles y mostrar el diagrama: AWS EC2, Azure Functions y Oracle. |
| 0:45–1:30 | Manuel | Justificar auditoría asíncrona. Mostrar trigger transaccional y explicar rollback/outbox. |
| 1:30–2:20 | Manuel | Mostrar PublicarEventos y el Topic real: esquema, endpoint sin clave y tipos publicados. |
| 2:20–3:30 | Manuel | Ejecutar creación, consulta y modificación de rol/usuario por REST; mostrar respuestas e ID. |
| 3:30–4:20 | Laura | Mostrar suscripción, filtros, destino AuditarEvento y contrato versión 1.0. |
| 4:20–5:20 | Laura | Mostrar el mismo event_id en outbox y auditoría; distinguir publicación y procesamiento. Explicar PK/idempotencia. |
| 5:20–6:20 | Laura | Demostrar mutación GraphQL y eliminación en orden usuario/rol; verificar eventos y errores de negocio. |
| 6:20–7:00 | Laura | Mostrar pruebas, Git y aportes reales de ambos. Concluir con desacoplamiento y límites de entrega. |

Antes de grabar, ejecutar todo el circuito. Publicar el video en Kaltura con permisos para el docente, probar el enlace y agregarlo al formato de respuesta. No subir la entrega a AVA hasta completar ese campo y verificar el repositorio.
