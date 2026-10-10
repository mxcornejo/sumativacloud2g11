package cl.duoc.sumativa.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import graphql.ErrorType;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.RuntimeWiring;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GraphQLSchemaTest {
  @Test
  void creationAcceptsNoRoleAndReturnsNullableRole() {
    java.util.Map<String,Object> user = new java.util.HashMap<>();
    user.put("id", "1"); user.put("fullName", "Test"); user.put("email", "test@example.test"); user.put("active", true); user.put("roleId", null);
    GraphQL gql = GraphQLSupport.build("/graphql/users.graphqls", RuntimeWiring.newRuntimeWiring()
        .type("Mutation", t -> t.dataFetcher("createUser", e -> user)).build());
    var result = gql.execute("mutation { createUser(input: {fullName: \"Test\", email: \"test@example.test\"}) { id roleId } }");
    assertEquals(0, result.getErrors().size());
    assertNotNull(result.getData());
  }

  @Test
  void usersSchemaExposesRequiredQueriesAndMutations() {
    GraphQL graphQL = GraphQLSupport.build("/graphql/users.graphqls", RuntimeWiring.newRuntimeWiring().build());
    GraphQLObjectType query = graphQL.getGraphQLSchema().getQueryType();
    GraphQLObjectType mutation = graphQL.getGraphQLSchema().getMutationType();

    assertNotNull(query.getFieldDefinition("users"));
    assertNotNull(query.getFieldDefinition("user"));
    assertNotNull(mutation.getFieldDefinition("createUser"));
    assertNotNull(mutation.getFieldDefinition("updateUser"));
    assertNotNull(mutation.getFieldDefinition("deleteUser"));
  }

  @Test
  void rolesSchemaExposesRequiredQueriesAndMutations() {
    GraphQL graphQL = GraphQLSupport.build("/graphql/roles.graphqls", RuntimeWiring.newRuntimeWiring().build());
    GraphQLObjectType query = graphQL.getGraphQLSchema().getQueryType();
    GraphQLObjectType mutation = graphQL.getGraphQLSchema().getMutationType();

    assertNotNull(query.getFieldDefinition("roles"));
    assertNotNull(query.getFieldDefinition("role"));
    assertNotNull(mutation.getFieldDefinition("createRole"));
    assertNotNull(mutation.getFieldDefinition("updateRole"));
    assertNotNull(mutation.getFieldDefinition("deleteRole"));
  }

  @Test
  void domainErrorsMapToExpectedHttpStatuses() {
    assertEquals(400, DomainErrors.status("INVALID_REQUEST").value());
    assertEquals(404, DomainErrors.status("USER_NOT_FOUND").value());
    assertEquals(409, DomainErrors.status("DUPLICATE_ROLE").value());
    assertEquals(409, DomainErrors.status("ROLE_IN_USE").value());
    assertEquals(500, DomainErrors.status("DATABASE_ERROR").value());
  }

  @Test
  @SuppressWarnings("unchecked")
  void validationErrorsExposeControlledCodeAndCorrelationId() {
    GraphQLError error = GraphqlErrorBuilder.newError()
        .message("Campo inválido")
        .errorType(ErrorType.ValidationError)
        .build();

    Map<String, Object> response = GraphQLSupport.specification(
        new ExecutionResultImpl(List.of(error)), "corr-test");
    Map<String, Object> firstError = ((List<Map<String, Object>>) response.get("errors")).get(0);
    Map<String, Object> extensions = (Map<String, Object>) firstError.get("extensions");

    assertEquals("GRAPHQL_VALIDATION_ERROR", extensions.get("code"));
    assertEquals("corr-test", extensions.get("correlationId"));
  }
}
