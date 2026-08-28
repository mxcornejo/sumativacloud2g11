# DSY2207 — Sumativa 1: Gestión de Usuarios y Roles

Solución cloud-native sin frontend para administrar usuarios y roles. Implementa CRUD de ambos recursos y mantiene la relación obligatoria entre un usuario y un rol existente.

## Arquitectura

`Postman → BFF Spring Boot en Docker/EC2 (AWS) → Azure Functions Java → Oracle Autonomous Database (OCI)`

El diagrama y la especificación se encuentran en [`diseno/`](diseno/).

## Contenido del repositorio

| Carpeta | Contenido |
|---|---|
| `bff-usuarios-roles/` | BFF Spring Boot Java 17, Dockerfile y pruebas. Expone `/api/users` y `/api/roles`. |
| `azure-functions-usuarios-roles/` | `UsuariosFunction` y `RolesFunction` en Azure Functions Java 17. |
| `diseno/` | Diagrama de arquitectura, especificación y script Oracle. |
| `postman/` | Colecciones para pruebas directas a Functions y mediante BFF/EC2. |

## Rutas

| Recurso | Operaciones |
|---|---|
| `/api/roles` y `/api/roles/{id}` | GET, POST, PUT, DELETE |
| `/api/users` y `/api/users/{id}` | GET, POST, PUT, DELETE |

## Ejecución local del BFF

Las URLs y claves de Azure Functions se configuran únicamente mediante variables de entorno:

```bash
export USERS_FUNCTION_URL='https://<function-app>.azurewebsites.net/api/users'
export USERS_FUNCTION_KEY='<clave-de-usuarios>'
export ROLES_FUNCTION_URL='https://<function-app>.azurewebsites.net/api/roles'
export ROLES_FUNCTION_KEY='<clave-de-roles>'

cd bff-usuarios-roles
mvn clean package
docker build -t usuarios-roles-bff:1.0 .
docker run --rm -p 8080:8080 \
  -e USERS_FUNCTION_URL -e USERS_FUNCTION_KEY \
  -e ROLES_FUNCTION_URL -e ROLES_FUNCTION_KEY \
  usuarios-roles-bff:1.0
```

Verificación: `GET http://localhost:8080/api/health`.

## Seguridad

El repositorio excluye deliberadamente Wallet Oracle, archivos `.pem`, claves de Functions, `.env`, artefactos de compilación y paquetes de despliegue. Usa los archivos de ejemplo y configura secretos solo en Azure Application Settings o variables de entorno de EC2.

## Pruebas

Importa `postman/Sumativa1_BFF_EC2_Usuarios_Roles.postman_collection.json` para validar el recorrido completo. Ejecuta primero el CRUD de roles y luego el de usuarios, ya que `users.role_id` requiere un rol existente.
