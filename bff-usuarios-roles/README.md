# BFF Usuarios y Roles

El BFF expone CRUD JSON en `/api/users` y `/api/roles`, y reenvía cada solicitud a la Azure Function correspondiente. Las URLs se configuran sin credenciales en código:

```bash
export USERS_FUNCTION_URL='https://<app>.azurewebsites.net/api/users'
export USERS_FUNCTION_KEY='<clave de UsuariosFunction>'
export ROLES_FUNCTION_URL='https://<app>.azurewebsites.net/api/roles'
export ROLES_FUNCTION_KEY='<clave de RolesFunction>'
mvn clean package
docker build -t usuarios-roles-bff:1.0 .
docker run --rm -p 8080:8080 -e USERS_FUNCTION_URL -e USERS_FUNCTION_KEY -e ROLES_FUNCTION_URL -e ROLES_FUNCTION_KEY usuarios-roles-bff:1.0
```

Use el encabezado opcional `X-Correlation-Id` para seguir una solicitud entre BFF y Functions. Si no se envía, el BFF genera uno.

Las claves de las Functions se almacenan solo como variables de entorno. No las agregue al repositorio, a Postman exportado ni a imágenes Docker.
