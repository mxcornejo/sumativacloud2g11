# Sumativa 3 · Aplicando tecnologías de eventos en arquitecturas cloud

**Grupo 11:** Manuel Cornejo y Laura Sarabia.
**Asignatura:** Desarrollo Cloud Native II (DSY2207).
**Carrera:** Ingeniería en Desarrollo de Software.
**Docente:** Ignacio Pastenet.
**Fecha de preparación:** 29 de septiembre de 2026.

## Problema y caso de uso EDA
El sistema anterior administra usuarios y roles mediante CRUD REST y GraphQL. Se incorpora un historial de cambios desacoplado: crear, actualizar o eliminar una entidad origina un evento que un consumidor transforma en un registro de auditoría. La consulta y la validación del CRUD siguen siendo síncronas; la auditoría es eventualmente consistente. No se usa mensajería para sustituir las restricciones de integridad de Oracle.

## Continuidad y responsabilidades
- BFF Spring Boot en Docker sobre AWS EC2: puerta de entrada REST/GraphQL existente.
- UsuariosFunction y RolesFunction: CRUD REST en Java.
- UsuariosGraphQLFunction y RolesGraphQLFunction: consultas y mutaciones GraphQL, reutilizando repositorios.
- Oracle: usuarios, roles y registro transaccional de cambios en eventos_outbox.
- PublicarEventos: función Java temporizada; construye el evento desde la outbox y lo publica por HTTPS en Azure Event Grid.
- Topic sumativa3-usuarios-roles-g11: distribuye los eventos admitidos por la suscripción.
- Suscripción auditoria-usuarios-roles: dirige seis tipos de evento a AuditarEvento.
- AuditarEvento: función Java con EventGridTrigger; valida el contrato y guarda una fila en auditoria_eventos.

## Flujo y decisiones
1. El cliente solicita un cambio por REST o GraphQL, mediante el BFF o la ruta de la función.
2. El repositorio ejecuta el CRUD. Un trigger Oracle añade la intención del evento en la misma transacción. Si la operación se revierte, también se revierte su evento.
3. Cada minuto PublicarEventos toma hasta 25 eventos pendientes y genera su sobre Event Grid. No publica datos personales: solo el identificador de la entidad y metadatos de la operación.
4. Tras la aceptación HTTP de Event Grid se marca published_at. Esto significa publicado, no procesado.
5. Event Grid invoca al consumidor. El registro con event_id único evita auditoría duplicada ante reentregas.
6. La unión entre outbox y auditoría permite comprobar el mismo ID de extremo a extremo.

Se utilizan triggers para cubrir tanto REST como GraphQL sin duplicar lógica en seis operaciones de cada protocolo. La función Java genera y publica el sobre; el trigger persiste la intención de dominio. Una caída después de publicar y antes de marcar la outbox puede repetir el envío: el ID estable y la clave primaria del consumidor resuelven ese caso. Los fallos de consumo se propagan para que Event Grid pueda reintentar. No se promete orden de entrega ni procesamiento global exactamente una vez.

## Contrato de eventos
Tipos: Usuarios.Created, Usuarios.Updated, Usuarios.Deleted, Roles.Created, Roles.Updated y Roles.Deleted. ID de 32 caracteres hexadecimales generado por Oracle. subject: /usuarios/{id} o /roles/{id}. eventTime: fecha UTC de la operación. dataVersion: 1.0. data: objeto con entityId numérico positivo. El consumidor rechaza tipos, versiones o identificadores inválidos y subjects inconsistentes.

## Configuración y ejecución
Aplicar diseno/oracle-eventos.sql una sola vez después del esquema de usuarios/roles. Cada bloque termina con una línea /. EventSchemaInitializer admite estos bloques y comprueba la compilación de los triggers. No usar SchemaInitializer para esta migración: separa sentencias por punto y coma y no admite PL/SQL.

Configurar EVENT_GRID_ENDPOINT, EVENT_GRID_KEY y OUTBOX_SCHEDULE=0 */1 * * * * junto con las variables Oracle existentes. Las credenciales y el wallet deben permanecer fuera del repositorio y del ZIP académico. El paquete privado de despliegue sí requiere el wallet para la conexión Oracle existente.

La suscripción debe usar endpoint-type azurefunction y apuntar al identificador del recurso /functions/AuditarEvento. Filtrar los seis tipos anteriores. Establecer 30 intentos y TTL de 1440 minutos. La revisión de fallos debe contemplar también los eventos publicados que aún no tengan auditoría.

## Pruebas y evidencia
Consultar EVIDENCIA.md para resultados efectivamente obtenidos. Las pruebas unitarias no sustituyen Oracle ni la entrega real de Event Grid. Para la demostración se deben mostrar CRUD REST y GraphQL, Event Grid Topic/suscripción, el ID publicado y el mismo ID procesado. EventEvidence permite consultar estos estados sin imprimir credenciales ni datos personales. Su opción rollback comprueba que el registro del evento se revierte con la operación.

## Límites y mejoras
La auditoría registra entidad, acción y tiempo; no identifica al operador ni conserva valores antes/después. No constituye un registro forense completo. La publicación procesa lotes pequeños para la carga académica. Un evento inválido al principio de la cola requiere intervención y puede bloquear el lote. La configuración de dead-letter y alertas, cuando no esté acreditada en EVIDENCIA.md, se considera pendiente de endurecimiento. El uso de clave compartida del Topic requiere rotación; una evolución futura puede usar identidad administrada. La retención/purga de outbox y auditoría debe definirse antes de un uso productivo.

## Video y trabajo colaborativo
El aviso vigente solicita Kaltura y participación equitativa de ambos integrantes. El formato contiene referencias heredadas a Teams. Se prepara una demostración de 4 a 8 minutos en Kaltura, conservando los campos del formato institucional. El enlace real debe agregarse después de grabar. La existencia del repositorio anterior no demuestra que los cambios nuevos estén publicados ni acredita participación de ambos: esas evidencias deben comprobarse por separado.

## Fuentes
- Pauta, formato y guía DSY2207 Exp3 S8 suministrados en sumativa 3.
- Aviso docente Semana 8 aportado por el estudiante.
- Microsoft Learn: https://learn.microsoft.com/en-us/azure/event-grid/delivery-and-retry
- Microsoft Learn: https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-event-grid-trigger

La fecha del aviso es inconsistente: indica lunes 05 de octubre y luego lunes 06 de octubre. Confirmar el cierre efectivo en AVA; no inferir una extensión.
