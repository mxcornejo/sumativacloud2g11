package cl.duoc.sumativa.bff.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.sumativa.bff.config.FunctionEndpointsProperties;
import cl.duoc.sumativa.bff.service.FunctionGateway;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

class GraphqlControllerTest {
  private final RecordingGateway gateway = new RecordingGateway();
  private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new GraphqlController(gateway)).build();

  @Test
  void forwardsUsersGraphqlAndPreservesCorrelationId() throws Exception {
    mvc.perform(post("/api/graphql/users")
            .contentType("application/json")
            .header("X-Correlation-Id", "graphql-123")
            .content("{\"query\":\"{ users { id } }\"}"))
        .andExpect(status().isOk());

    assertEquals("users", gateway.domain);
    assertEquals("graphql-123", gateway.correlationId);
  }

  @Test
  void rejectsGraphqlWithoutBody() throws Exception {
    mvc.perform(post("/api/graphql/roles").header("X-Correlation-Id", "graphql-456"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("X-Correlation-Id", "graphql-456"));
  }

  private static final class RecordingGateway extends FunctionGateway {
    String domain;
    String correlationId;

    RecordingGateway() {
      super(new RestTemplate(), new FunctionEndpointsProperties(
          "http://unused/users", "", "http://unused/roles", "",
          "http://unused/graphql/users", "", "http://unused/graphql/roles", ""));
    }

    @Override
    public ResponseEntity<String> forwardGraphql(String domain, String body, String correlationId) {
      this.domain = domain;
      this.correlationId = correlationId;
      return ResponseEntity.ok("{\"data\":{}}");
    }
  }
}
