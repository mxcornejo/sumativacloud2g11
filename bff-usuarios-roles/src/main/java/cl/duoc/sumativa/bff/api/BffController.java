package cl.duoc.sumativa.bff.api;

import cl.duoc.sumativa.bff.service.FunctionGateway;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BffController {
  private final FunctionGateway gateway;

  public BffController(FunctionGateway gateway) {
    this.gateway = gateway;
  }

  @GetMapping("/health")
  public ResponseEntity<?> health(@RequestHeader(value = "X-Correlation-Id", required = false) String incomingId) {
    String correlationId = correlationId(incomingId);
    return ResponseEntity.ok().header("X-Correlation-Id", correlationId)
        .body(java.util.Map.of("status", "ok", "service", "usuarios-roles-bff", "correlationId", correlationId));
  }

  @GetMapping({"/users", "/roles"})
  public ResponseEntity<String> list(org.springframework.web.context.request.WebRequest request,
      @RequestHeader(value = "X-Correlation-Id", required = false) String incomingId) {
    return gateway.forward(domain(request), HttpMethod.GET, null, null, correlationId(incomingId));
  }

  @GetMapping({"/users/{id}", "/roles/{id}"})
  public ResponseEntity<String> get(@PathVariable String id, org.springframework.web.context.request.WebRequest request,
      @RequestHeader(value = "X-Correlation-Id", required = false) String incomingId) {
    return gateway.forward(domain(request), HttpMethod.GET, id, null, correlationId(incomingId));
  }

  @PostMapping({"/users", "/roles"})
  public ResponseEntity<?> create(@RequestBody(required = false) String body, org.springframework.web.context.request.WebRequest request,
      @RequestHeader(value = "X-Correlation-Id", required = false) String incomingId) {
    String correlationId = correlationId(incomingId);
    if (!StringUtils.hasText(body)) return badRequest(correlationId);
    return gateway.forward(domain(request), HttpMethod.POST, null, body, correlationId);
  }

  @PutMapping({"/users/{id}", "/roles/{id}"})
  public ResponseEntity<?> update(@PathVariable String id, @RequestBody(required = false) String body,
      org.springframework.web.context.request.WebRequest request, @RequestHeader(value = "X-Correlation-Id", required = false) String incomingId) {
    String correlationId = correlationId(incomingId);
    if (!StringUtils.hasText(body)) return badRequest(correlationId);
    return gateway.forward(domain(request), HttpMethod.PUT, id, body, correlationId);
  }

  @DeleteMapping({"/users/{id}", "/roles/{id}"})
  public ResponseEntity<String> delete(@PathVariable String id, org.springframework.web.context.request.WebRequest request,
      @RequestHeader(value = "X-Correlation-Id", required = false) String incomingId) {
    return gateway.forward(domain(request), HttpMethod.DELETE, id, null, correlationId(incomingId));
  }

  private String domain(org.springframework.web.context.request.WebRequest request) {
    return request.getDescription(false).contains("/roles") ? "roles" : "users";
  }

  private String correlationId(String incomingId) { return StringUtils.hasText(incomingId) ? incomingId : UUID.randomUUID().toString(); }
  private ResponseEntity<ApiError> badRequest(String id) { return ResponseEntity.badRequest().header("X-Correlation-Id", id).body(ApiError.of(400, "INVALID_REQUEST", "El cuerpo JSON es obligatorio.", id)); }
}
