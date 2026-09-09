# DSY2207 - Sumativa 2: REST y GraphQL

Sistema de Gestión de Usuarios y Roles desarrollado por el Grupo 11: Manuel Cornejo y Laura Sarabia.

La solución continúa el trabajo de la Sumativa 1 y agrega dos APIs GraphQL sin reemplazar los CRUD REST existentes. Docker Labs no forma parte de esta entrega: el contenedor Spring Boot se ejecuta en una instancia EC2 de AWS.

## Arquitectura

```text
Postman
  -> BFF Spring Boot en Docker / EC2
      -> UsuariosFunction (REST) -----------\
      -> RolesFunction (REST) ---------------+-> Oracle Database
      -> UsuariosGraphQLFunction (GraphQL) --+
      -> RolesGraphQLFunction (GraphQL) -----/
```

El BFF protege las claves de Azure Functions, propaga `X-Correlation-Id` y mantiene las URL en variables de entorno. Las cuatro Functions son stateless y comparten los mismos repositorios y reglas de negocio Oracle.

## Componentes

- `azure-functions-usuarios-roles/`: cuatro Azure Functions Java 17, dos REST y dos GraphQL.
- `bff-usuarios-roles/`: BFF Spring Boot 3.3.5 desplegado con Docker en EC2.
- `postman/`: colecciones de validación del recorrido directo y del recorrido completo por EC2.
- `diseno/`: modelo Oracle y diagrama editable de arquitectura.
- `docs/`: guion del video y lista de evidencias.

## API pública del BFF

| Protocolo | Método y ruta | Propósito |
|---|---|---|
| REST | `GET/POST /api/users` | Listar y crear usuarios |
| REST | `GET/PUT/DELETE /api/users/{id}` | Consultar, actualizar y eliminar usuario |
| REST | `GET/POST /api/roles` | Listar y crear roles |
| REST | `GET/PUT/DELETE /api/roles/{id}` | Consultar, actualizar y eliminar rol |
| GraphQL | `POST /api/graphql/users` | Queries y mutations de usuarios |
| GraphQL | `POST /api/graphql/roles` | Queries y mutations de roles |
| Operación | `GET /api/health` | Salud del BFF |

Las solicitudes GraphQL usan el formato:

```json
{
  "query": "query ListaUsuarios { users { id fullName email roleId active } }",
  "variables": {},
  "operationName": "ListaUsuarios"
}
```

## Compilación y pruebas

```bash
cd azure-functions-usuarios-roles
mvn clean test package

cd ../bff-usuarios-roles
mvn clean test package
```

El paquete Azure debe generar cuatro carpetas dentro de `target/azure-functions/usuarios-roles-functions`: `UsuariosFunction`, `RolesFunction`, `UsuariosGraphQLFunction` y `RolesGraphQLFunction`.

## Configuración

Azure Functions requiere `ORACLE_DB_URL`, `ORACLE_DB_USER` y `ORACLE_DB_PASSWORD`, además del Wallet Oracle provisto únicamente durante el despliegue. El BFF requiere:

- `USERS_FUNCTION_URL` y `USERS_FUNCTION_KEY`
- `ROLES_FUNCTION_URL` y `ROLES_FUNCTION_KEY`
- `USERS_GRAPHQL_FUNCTION_URL` y `USERS_GRAPHQL_FUNCTION_KEY`
- `ROLES_GRAPHQL_FUNCTION_URL` y `ROLES_GRAPHQL_FUNCTION_KEY`

No guardar valores reales en Git, archivos Postman, capturas ni grabaciones.

## Despliegue sin Docker Labs

1. Desplegar el paquete Java en la Function App de Azure y comprobar las cuatro rutas directas.
2. Compilar el BFF y construir la imagen `usuarios-roles-bff:2.0`.
3. Transferir el BFF a la EC2 y ejecutar el contenedor en `80:8080` con reinicio automático.
4. Inyectar las URL y claves mediante variables de entorno del contenedor.
5. Ejecutar la colección `Sumativa2_BFF_EC2_REST_GraphQL.postman_collection.json`.

## Seguridad y limpieza

El `.gitignore` excluye Wallets, llaves `.pem`, contraseñas, archivos `.env`, `target/` y paquetes de despliegue. Para limpiar las pruebas se debe eliminar primero el usuario y después el rol, porque `usuarios.role_id` es una clave foránea obligatoria.
