package cl.duoc.sumativa.functions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UsuariosFunction {
  @FunctionName("UsuariosFunction")
  public HttpResponseMessage run(
      @HttpTrigger(name = "request", methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
          authLevel = AuthorizationLevel.FUNCTION, route = "users/{id?}")
      HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    String correlationId = request.getHeaders().getOrDefault("x-correlation-id", UUID.randomUUID().toString());
    String id = RouteParameter.id(request.getUri(), "users");
    try (Connection connection = OracleConnection.open()) {
      UserRepository users = new UserRepository(connection);
      return switch (request.getHttpMethod()) {
        case GET -> id == null
            ? JsonResponses.body(request, HttpStatus.OK, users.list(), correlationId)
            : JsonResponses.body(request, HttpStatus.OK, required(users.find(parseId(id)), "USER_NOT_FOUND", "Usuario no encontrado."), correlationId);
        case POST -> JsonResponses.body(request, HttpStatus.CREATED, users.create(body(request)), correlationId);
        case PUT -> {
          if (id == null) throw new DomainException("ID_REQUIRED", "El id del usuario es obligatorio.");
          yield JsonResponses.body(request, HttpStatus.OK, users.update(parseId(id), body(request)), correlationId);
        }
        case DELETE -> {
          if (id == null) throw new DomainException("ID_REQUIRED", "El id del usuario es obligatorio.");
          users.delete(parseId(id));
          yield request.createResponseBuilder(HttpStatus.NO_CONTENT).header("X-Correlation-Id", correlationId).build();
        }
        default -> JsonResponses.error(request, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Método no permitido.", correlationId);
      };
    } catch (DomainException exception) {
      return JsonResponses.error(request, DomainErrors.status(exception.code()), exception.code(), exception.getMessage(), correlationId);
    } catch (IllegalStateException exception) {
      context.getLogger().severe(exception.getMessage());
      return JsonResponses.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "CONFIGURATION_ERROR", "Configuración de base de datos incompleta.", correlationId);
    } catch (SQLException exception) {
      context.getLogger().severe(exception.getMessage());
      return JsonResponses.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "No fue posible procesar la operación.", correlationId);
    }
  }

  private static Map<String, Object> body(HttpRequestMessage<Optional<String>> request) {
    try {
      return JsonResponses.JSON.readValue(request.getBody().orElseThrow(), new TypeReference<>() {});
    } catch (Exception exception) {
      throw new DomainException("INVALID_REQUEST", "El cuerpo debe contener un JSON válido.");
    }
  }

  private static long parseId(String value) {
    try {
      long id = Long.parseLong(value);
      if (id <= 0) throw new NumberFormatException();
      return id;
    } catch (NumberFormatException exception) {
      throw new DomainException("INVALID_ID", "Id inválido.");
    }
  }

  private static Map<String, Object> required(Map<String, Object> value, String code, String message) {
    if (value == null) throw new DomainException(code, message);
    return value;
  }
}
