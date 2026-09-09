package cl.duoc.sumativa.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** URLs y claves de las Azure Functions. Se sobrescriben con variables de entorno en cada ambiente. */
@ConfigurationProperties(prefix = "functions")
public record FunctionEndpointsProperties(
    String usersUrl,
    String usersKey,
    String rolesUrl,
    String rolesKey,
    String usersGraphqlUrl,
    String usersGraphqlKey,
    String rolesGraphqlUrl,
    String rolesGraphqlKey) {}
