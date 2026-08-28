# Azure Functions — Usuarios y Roles

Contiene dos funciones HTTP Java 17:

- `UsuariosFunction`, ruta `/api/users/{id?}`.
- `RolesFunction`, ruta `/api/roles/{id?}`.

Ambas implementan `GET`, `POST`, `PUT` y `DELETE`, devuelven JSON y utilizan `X-Correlation-Id`. Configurar las credenciales Oracle exclusivamente como Application Settings: `ORACLE_DB_URL`, `ORACLE_DB_USER` y `ORACLE_DB_PASSWORD`. El Wallet se empaqueta privado con la Function y se excluye de Git.

## Preparación y publicación

```bash
mvn clean package
mvn azure-functions:deploy
```

Antes de publicar, configurar un Function App Java 17 y el grupo de recursos mediante `AZURE_RESOURCE_GROUP`. El despliegue puede realizarse desde VS Code con la extensión Azure Functions o mediante Maven/Azure CLI.

## Crear las tablas por terminal

Con el Wallet extraído en `.oracle-wallet`, ejecutar desde esta carpeta. La contraseña se solicita en la terminal y no se guarda:

```bash
read -s ORACLE_DB_PASSWORD
export ORACLE_DB_PASSWORD ORACLE_DB_USER=ADMIN ORACLE_DB_URL='jdbc:oracle:thin:@b9nhv92q7bji4s88_high'
mvn -q -Dexec.mainClass=cl.duoc.sumativa.functions.SchemaInitializer org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```
