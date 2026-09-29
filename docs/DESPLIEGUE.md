# Despliegue y recuperación

Entorno verificado: Azure for Students, grupo SUMATIVA1_USUARIOS_ROLES, Function App sumativa1usuariosrolesmc2026, Java 17, Flex Consumption, Brazil South. Se reutiliza Oracle y el BFF EC2 anteriores.

## Reproducir con acceso autorizado
1. Autenticarse en Azure y confirmar la suscripción. Guardar un paquete conocido de la versión anterior antes de reemplazar una aplicación en uso.
2. Compilar en azure-functions-usuarios-roles con Java 17: `mvn test package`.
3. Preparar Oracle con diseno/oracle-eventos.sql una sola vez, usando SQLcl/SQL Developer o EventSchemaInitializer. No volver a ejecutar CREATE TABLE sobre un esquema ya migrado.
4. Configurar las variables Oracle anteriores, AzureWebJobsStorage, EVENT_GRID_ENDPOINT, EVENT_GRID_KEY y OUTBOX_SCHEDULE. Nunca imprimirlas ni agregarlas al repositorio.
5. Crear un paquete privado con el contenido de target/azure-functions/usuarios-roles-functions. El wallet de la base debe incluirse de forma privada en el JAR o estar disponible en la ruta configurada. Nunca incorporar ese paquete privado al ZIP académico.
6. Desplegar usando `az functionapp deployment source config-zip -g SUMATIVA1_USUARIOS_ROLES -n sumativa1usuariosrolesmc2026 --src <paquete-privado.zip>`.
7. Comprobar las seis funciones. Ejecutar infra/azure-eventgrid.sh con las variables indicadas en su cabecera para crear la suscripción.
8. Ejecutar docs/PRUEBAS.md y correlacionar IDs de outbox/auditoría.

El paquete académico contiene código fuente y configuración de ejemplo; por seguridad no es un ZIP listo para desplegar con credenciales.

## Recuperación
Si falla la publicación, la outbox conserva los eventos pendientes. Corregir conectividad/configuración y verificar el siguiente ciclo. Si Event Grid acepta el evento y el proceso cae antes de marcarlo, puede repetirse el mismo ID; el consumidor deduplica.

Si una actualización de funciones falla, restaurar un paquete previamente validado. Para detener temporalmente el publicador configurar AzureWebJobs.PublicarEventos.Disabled=true. Los triggers siguen guardando intenciones pendientes; monitorizar crecimiento. No borrar tablas, triggers ni datos de usuarios para solucionar un error de despliegue. Al reanudar, quitar la desactivación y comprobar auditoría. La restauración y las pruebas de desastre no se ejecutaron como parte de esta entrega.

## Git
Los cambios se preparan en la rama sumativa-3-event-grid del repositorio de continuidad. Revisar su commit y su publicación antes del video. Distribuir revisión y demostración entre ambos integrantes; no fabricar autorías.
