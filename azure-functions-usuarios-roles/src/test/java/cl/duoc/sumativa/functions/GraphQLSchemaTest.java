package cl.duoc.sumativa.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import graphql.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.RuntimeWiring;
import org.junit.jupiter.api.Test;

class GraphQLSchemaTest {
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
}
