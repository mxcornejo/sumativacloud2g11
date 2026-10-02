# Consulta en vivo para el video (Java)

Desde la terminal de VS Code, en la carpeta sumativa 1:

```bash
./ver-auditoria.sh 61 61
```

Reemplazar el primer número por roleId y el segundo por userId de Postman. El script de terminal inicia **infra/VerAuditoria.java con Java 17**. No utiliza el script Python anterior ni modifica Azure, el BFF o datos Oracle. Usa Azure CLI para leer la configuración mediante la sesión autenticada del equipo; Azure CLI es una herramienta externa con su propio runtime.

La tabla se consulta directamente en Oracle cada vez que se ejecuta el comando. Muestra EVENT_ID, EVENT_TYPE, SUBJECT, PUBLICACION y CONSUMO. Si hay pendientes, esperar el siguiente ciclo y repetir. Se esperan seis eventos solo si se creó, modificó una vez y eliminó cada entidad.

Requisitos del equipo: Java 17, Azure CLI con sesión activa, wallet en azure-functions-usuarios-roles/.oracle-wallet y dependencias compiladas en target. Si falta sesión, usar az login antes de grabar. No mostrar claves o el wallet. Las credenciales se leen en un directorio temporal privado y se eliminan al finalizar.

Texto para decir: “Ejecutamos nuestra herramienta local en Java para consultar Oracle en tiempo real. Filtramos los eventos por los IDs del rol y usuario creados durante esta demostración y comprobamos su publicación y procesamiento”.
