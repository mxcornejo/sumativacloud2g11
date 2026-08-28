package cl.duoc.sumativa.functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import java.time.Instant;
import java.util.Map;

final class JsonResponses {
  static final ObjectMapper JSON = new ObjectMapper();
  private JsonResponses() {}
  static HttpResponseMessage body(HttpRequestMessage<?> request, HttpStatus status, Object value, String correlationId) {
    try { return request.createResponseBuilder(status).header("Content-Type", "application/json").header("X-Correlation-Id", correlationId).body(JSON.writeValueAsString(value)).build(); }
    catch (Exception exception) { return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"code\":\"SERIALIZATION_ERROR\"}").build(); }
  }
  static HttpResponseMessage error(HttpRequestMessage<?> request, HttpStatus status, String code, String message, String correlationId) {
    return body(request, status, Map.of("timestamp", Instant.now().toString(), "status", status.value(), "code", code, "message", message, "correlationId", correlationId), correlationId);
  }
}
