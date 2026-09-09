# Guion de video - Sumativa 2 DSY2207

Duración objetivo: 6 a 7 minutos. Participantes: Manuel Cornejo y Laura Sarabia.

## 1. Presentación y arquitectura - Manuel (0:00 a 0:45)

**Mostrar:** diagrama de arquitectura.

“Hola, somos Manuel Cornejo y Laura Sarabia, del Grupo 11. Presentaremos la Sumativa 2 del Sistema de Gestión de Usuarios y Roles. Continuamos la solución anterior incorporando dos capas REST y dos capas GraphQL sobre cuatro Azure Functions Java.”

“Postman consume un BFF Spring Boot desplegado como contenedor Docker en una EC2 de AWS. El BFF invoca las cuatro Functions y estas trabajan con Oracle. En esta entrega no utilizamos Docker Labs; Docker se ejecuta directamente en EC2.”

## 2. Funciones REST - Laura (0:45 a 1:40)

**Mostrar:** Azure Portal con `UsuariosFunction` y `RolesFunction`, luego sus rutas.

“Las funciones REST mantienen el CRUD completo. `UsuariosFunction` responde en `/api/users` y `RolesFunction` en `/api/roles`. GET consulta, POST crea, PUT actualiza y DELETE elimina.”

“Ambas funciones son stateless y usan repositorios compartidos. Oracle asegura que el correo y el nombre del rol sean únicos, y que cada usuario tenga un rol existente.”

## 3. Funciones GraphQL - Manuel (1:40 a 2:45)

**Mostrar:** los dos archivos `.graphqls` y Azure Portal con las Functions GraphQL.

“Agregamos `UsuariosGraphQLFunction` y `RolesGraphQLFunction`. Cada una recibe solicitudes POST con `query`, `variables` y `operationName`.”

“GraphQL permite pedir solamente los campos necesarios. En usuarios implementamos `users`, `user`, `createUser`, `updateUser` y `deleteUser`. En roles implementamos las operaciones equivalentes.”

## 4. REST en tiempo real - Laura (2:45 a 3:45)

**Mostrar y ejecutar en Postman:** crear rol, crear usuario y listar usuarios mediante la URL de EC2.

“Primero creamos un rol con REST y recibimos `201 Created`. Después creamos un usuario asociado a ese rol. Finalmente consultamos la lista y verificamos que los datos fueron almacenados en Oracle.”

**Ejecutar:** eliminar el rol mientras el usuario todavía existe.

“Esta petición devuelve `409 Conflict`, porque no se permite eliminar un rol que está siendo utilizado.”

## 5. GraphQL en tiempo real - Manuel (3:45 a 5:00)

**Mostrar y ejecutar en Postman:** query de roles y query de usuarios; luego una mutation de actualización.

“Con esta query pedimos solo identificador y nombre de los roles. La respuesta aparece dentro de `data`. Ahora actualizamos el usuario mediante una mutation y solicitamos únicamente los campos necesarios.”

**Ejecutar consulta inválida.**

“GraphQL responde mediante `errors` y entrega un código controlado, sin mostrar sentencias SQL ni credenciales.”

## 6. EC2, seguridad y observabilidad - Laura (5:00 a 5:55)

**Mostrar:** instancia EC2 encendida, `docker ps` y `/api/health`.

“El BFF está dentro del contenedor `usuarios-roles-bff` y publica el puerto 80. Las URL y claves de Azure se inyectan como variables privadas del contenedor y no están en Git.”

“Cada solicitud usa `X-Correlation-Id`, lo que permite relacionar la llamada de Postman, el BFF y los registros de Azure.”

## 7. Git y cierre - Manuel y Laura (5:55 a 6:40)

**Mostrar:** rama `sumativa-2`, commits, Pull Request y README.

**Manuel:** “El repositorio conserva la historia de la Sumativa 1 y agrega la implementación REST y GraphQL, pruebas, Postman y documentación en una rama específica.”

**Laura:** “La solución cumple el CRUD de usuarios y roles mediante APIs serverless, mantiene responsabilidades separadas y utiliza Oracle como persistencia común.”

**Ambos:** “Gracias.”

## Precauciones antes de grabar

- No mostrar claves de Functions, contraseña Oracle, Wallet, archivo `runtime.env` ni llave `.pem`.
- Preparar datos con nombres y correos únicos.
- Verificar previamente que `/api/health`, REST y GraphQL respondan.
- Limpiar los datos eliminando primero el usuario y después el rol.
- Mantener la duración entre 3 y 8 minutos.
