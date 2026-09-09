package cl.duoc.sumativa.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UsuariosGraphQLFunction {
  private static final GraphQL GRAPHQL = GraphQLSupport.build("/graphql/users.graphqls", wiring());

  @FunctionName("UsuariosGraphQLFunction")
  public HttpResponseMessage run(
      @HttpTrigger(name = "request", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION,
          route = "graphql/users") HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    String correlationId = request.getHeaders().getOrDefault("x-correlation-id", UUID.randomUUID().toString());
    try (Connection connection = OracleConnection.open()) {
      return GraphQLSupport.execute(GRAPHQL, request, connection, correlationId);
    } catch (IllegalStateException exception) {
      context.getLogger().severe(exception.getMessage());
      return JsonResponses.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "CONFIGURATION_ERROR", "Configuración de base de datos incompleta.", correlationId);
    } catch (SQLException exception) {
      context.getLogger().severe(exception.getMessage());
      return JsonResponses.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "No fue posible procesar la operación.", correlationId);
    }
  }

  private static RuntimeWiring wiring() {
    return RuntimeWiring.newRuntimeWiring()
        .type("Query", type -> type
            .dataFetcher("users", environment -> users(environment).list())
            .dataFetcher("user", environment -> users(environment).find(GraphQLSupport.id(environment.getArgument("id")))))
        .type("Mutation", type -> type
            .dataFetcher("createUser", environment -> users(environment).create(GraphQLSupport.input(environment)))
            .dataFetcher("updateUser", environment -> users(environment).update(
                GraphQLSupport.id(environment.getArgument("id")), GraphQLSupport.input(environment)))
            .dataFetcher("deleteUser", environment -> {
              users(environment).delete(GraphQLSupport.id(environment.getArgument("id")));
              return true;
            }))
        .build();
  }

  private static UserRepository users(graphql.schema.DataFetchingEnvironment environment) {
    return new UserRepository(GraphQLSupport.connection(environment));
  }
}
