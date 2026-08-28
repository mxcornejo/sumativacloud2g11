package cl.duoc.sumativa.bff.api;

import cl.duoc.sumativa.bff.service.FunctionGateway;
import cl.duoc.sumativa.bff.config.FunctionEndpointsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BffControllerTest {
  private final RecordingGateway gateway = new RecordingGateway();
  private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BffController(gateway)).build();

  @Test
  void routesRolesToTheRolesFunctionAndPreservesCorrelationId() throws Exception {
    mvc.perform(get("/api/roles").header("X-Correlation-Id", "trace-123"))
        .andExpect(status().isOk());

    org.junit.jupiter.api.Assertions.assertEquals("roles", gateway.domain);
    org.junit.jupiter.api.Assertions.assertEquals(HttpMethod.GET, gateway.method);
    org.junit.jupiter.api.Assertions.assertEquals("trace-123", gateway.correlationId);
  }

  @Test
  void rejectsCreateWithoutJsonBody() throws Exception {
    mvc.perform(post("/api/users").header("X-Correlation-Id", "trace-456"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("X-Correlation-Id", "trace-456"));
  }

  private static final class RecordingGateway extends FunctionGateway {
    String domain;
    HttpMethod method;
    String correlationId;

    RecordingGateway() {
      super(new RestTemplate(), new FunctionEndpointsProperties("http://unused/users", "", "http://unused/roles", ""));
    }

    @Override
    public ResponseEntity<String> forward(String domain, HttpMethod method, String resourceId, String body, String correlationId) {
      this.domain = domain;
      this.method = method;
      this.correlationId = correlationId;
      return ResponseEntity.ok("[]");
    }
  }
}
