# BFF Usuarios y Roles - EC2

Microservicio Spring Boot Java 17 que expone REST y GraphQL, y reenvía las solicitudes a Azure Functions. Se despliega como contenedor Docker en EC2; Docker Labs no se utiliza.

## Variables de entorno

```text
USERS_FUNCTION_URL
USERS_FUNCTION_KEY
ROLES_FUNCTION_URL
ROLES_FUNCTION_KEY
USERS_GRAPHQL_FUNCTION_URL
USERS_GRAPHQL_FUNCTION_KEY
ROLES_GRAPHQL_FUNCTION_URL
ROLES_GRAPHQL_FUNCTION_KEY
```

El tiempo máximo de lectura es 45 segundos para tolerar arranques en frío de Azure y Oracle. Las claves se envían mediante `x-functions-key`, y `X-Correlation-Id` se conserva en todo el recorrido.

## Construcción

```bash
mvn clean test package
docker build -t usuarios-roles-bff:2.0 .
```

## EC2

Ejecutar con el archivo de variables privado de la instancia:

```bash
docker run -d --name usuarios-roles-bff --restart unless-stopped \
  -p 80:8080 --env-file /home/ec2-user/.config/usuarios-roles-bff/runtime.env \
  usuarios-roles-bff:2.0
```

Comprobar `GET /api/health`, después una consulta REST y las dos consultas GraphQL antes de grabar el video.
