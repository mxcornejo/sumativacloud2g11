package cl.duoc.sumativa.functions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class GraphQLSupport {
  private GraphQLSupport() {}

  static GraphQL build(String schemaResource, RuntimeWiring wiring) {
    TypeDefinitionRegistry registry = new SchemaParser().parse(read(schemaResource));
    return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(registry, wiring))
        .defaultDataFetcherExceptionHandler(new SafeExceptionHandler())
        .build();
  }

  static HttpResponseMessage execute(
      GraphQL graphQL,
      HttpRequestMessage<Optional<String>> request,
      Connection connection,
      String correlationId) {
    try {
      Map<String, Object> payload = JsonResponses.JSON.readValue(
          request.getBody().orElseThrow(), new TypeReference<>() {});
      Object queryValue = payload.get("query");
      if (queryValue == null || queryValue.toString().isBlank()) {
        return invalidRequest(request, correlationId, "query es obligatorio.");
      }
      Map<String, Object> variables = variables(payload.get("variables"));
      ExecutionInput.Builder input = ExecutionInput.newExecutionInput()
          .query(queryValue.toString())
          .variables(variables)
          .graphQLContext(builder -> builder.of("connection", connection, "correlationId", correlationId));
      Object operationName = payload.get("operationName");
      if (operationName != null && !operationName.toString().isBlank()) input.operationName(operationName.toString());
      ExecutionResult result = graphQL.execute(input.build());
      return JsonResponses.body(request, HttpStatus.OK, result.toSpecification(), correlationId);
    } catch (Exception exception) {
      return invalidRequest(request, correlationId, "El cuerpo debe contener una solicitud GraphQL válida.");
    }
  }

  static Connection connection(graphql.schema.DataFetchingEnvironment environment) {
    return environment.getGraphQlContext().get("connection");
  }

  static long id(Object value) {
    try {
      long id = Long.parseLong(String.valueOf(value));
      if (id <= 0) throw new NumberFormatException();
      return id;
    } catch (NumberFormatException exception) {
      throw new DomainException("INVALID_ID", "El identificador debe ser un número positivo.");
    }
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> input(graphql.schema.DataFetchingEnvironment environment) {
    Object value = environment.getArgument("input");
    if (!(value instanceof Map<?, ?>)) throw new DomainException("INVALID_REQUEST", "input es obligatorio.");
    return (Map<String, Object>) value;
  }

  private static Map<String, Object> variables(Object value) {
    if (value == null) return Map.of();
    if (!(value instanceof Map<?, ?> map)) throw new DomainException("INVALID_REQUEST", "variables debe ser un objeto JSON.");
    Map<String, Object> variables = new LinkedHashMap<>();
    map.forEach((key, item) -> variables.put(String.valueOf(key), item));
    return variables;
  }

  private static HttpResponseMessage invalidRequest(
      HttpRequestMessage<?> request, String correlationId, String message) {
    Map<String, Object> error = Map.of(
        "message", message,
        "extensions", Map.of("code", "INVALID_REQUEST", "correlationId", correlationId));
    return JsonResponses.body(request, HttpStatus.BAD_REQUEST, Map.of("errors", java.util.List.of(error)), correlationId);
  }

  private static String read(String resource) {
    try (InputStream input = GraphQLSupport.class.getResourceAsStream(resource)) {
      if (input == null) throw new IllegalStateException("No se encontró el esquema GraphQL " + resource);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("No fue posible leer el esquema GraphQL.", exception);
    }
  }

  private static final class SafeExceptionHandler implements DataFetcherExceptionHandler {
    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
        DataFetcherExceptionHandlerParameters parameters) {
      Throwable exception = unwrap(parameters.getException());
      String code = exception instanceof DomainException domain ? domain.code() : "INTERNAL_ERROR";
      String message = exception instanceof DomainException ? exception.getMessage() : "No fue posible completar la operación.";
      GraphQLError error = graphql.GraphqlErrorBuilder.newError()
          .message(message)
          .path(parameters.getPath())
          .location(parameters.getSourceLocation())
          .extensions(Map.of("code", code))
          .build();
      return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult().error(error).build());
    }

    private static Throwable unwrap(Throwable exception) {
      Throwable current = exception;
      while (current.getCause() != null && !(current instanceof DomainException)) current = current.getCause();
      return current;
    }
  }
}
