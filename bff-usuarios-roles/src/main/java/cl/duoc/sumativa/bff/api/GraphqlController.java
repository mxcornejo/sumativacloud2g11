package cl.duoc.sumativa.bff.api;

import cl.duoc.sumativa.bff.service.FunctionGateway;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graphql")
public class GraphqlController {
  private final FunctionGateway gateway;

  public GraphqlController(FunctionGateway gateway) {
    this.gateway = gateway;
  }

  @PostMapping("/users")
  public ResponseEntity<?> users(
      @RequestBody(required = false) String body,
      @RequestHeader(value = "X-Correlation-Id", required = false) String incomingId) {
    return forward("users", body, incomingId);
  }

  @PostMapping("/roles")
  public ResponseEntity<?> roles(
      @RequestBody(required = false) String body,
      @RequestHeader(value = "X-Correlation-Id", required = false) String incomingId) {
    return forward("roles", body, incomingId);
  }

  private ResponseEntity<?> forward(String domain, String body, String incomingId) {
    String correlationId = StringUtils.hasText(incomingId) ? incomingId : UUID.randomUUID().toString();
    if (!StringUtils.hasText(body)) {
      return ResponseEntity.badRequest().header("X-Correlation-Id", correlationId)
          .body(ApiError.of(400, "INVALID_REQUEST", "El cuerpo GraphQL es obligatorio.", correlationId));
    }
    return gateway.forwardGraphql(domain, body, correlationId);
  }
}
