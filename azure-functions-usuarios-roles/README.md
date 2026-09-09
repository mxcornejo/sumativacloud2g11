# Azure Functions - REST y GraphQL

Function App Java 17 con cuatro funciones HTTP:

- `UsuariosFunction`: CRUD REST en `/api/users/{id?}`.
- `RolesFunction`: CRUD REST en `/api/roles/{id?}`.
- `UsuariosGraphQLFunction`: GraphQL en `/api/graphql/users`.
- `RolesGraphQLFunction`: GraphQL en `/api/graphql/roles`.

Las funciones comparten `UserRepository` y `RoleRepository`; todas persisten en Oracle y aplican las mismas validaciones. GraphQL usa `graphql-java` y carga los esquemas desde `src/main/resources/graphql`.

## Configuración local

Copiar `local.settings.sample.json` como `local.settings.json` y completar los valores reales solo en el archivo ignorado. El Wallet debe ubicarse fuera del repositorio o en `.oracle-wallet`, también ignorado.

## Verificación

```bash
mvn clean test package
find target/azure-functions/usuarios-roles-functions -name function.json -maxdepth 2
```

Las rutas publicadas usan autorización `FUNCTION`; las claves deben viajar en `x-functions-key` y nunca quedar en el código ni en una URL registrada.
