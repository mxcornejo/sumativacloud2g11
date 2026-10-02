package cl.duoc.sumativa.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

final class DomainEvent {
  static final ObjectMapper JSON = new ObjectMapper();
  static final Set<String> TYPES = Set.of("Usuarios.Created", "Usuarios.Updated", "Usuarios.Deleted", "Roles.Created", "Roles.Updated", "Roles.Deleted");
  static String encode(String id, String type, String subject, Instant time, String payload) throws Exception {
    JsonNode data = JSON.readTree(payload);
    String result = JSON.writeValueAsString(Map.of("id", id, "eventType", type, "subject", subject,
        "eventTime", time.toString(), "dataVersion", "1.0", "data", data));
    validate(result);
    return result;
  }
  static JsonNode validate(String raw) throws Exception {
    JsonNode e = JSON.readTree(raw);
    if (e == null || !e.isObject() || !e.path("id").asText().matches("[A-Fa-f0-9-]{32,36}")
        || !TYPES.contains(e.path("eventType").asText()) || !"1.0".equals(e.path("dataVersion").asText())
        || !e.path("data").path("entityId").isIntegralNumber() || e.path("data").path("entityId").asLong() <= 0)
      throw new IllegalArgumentException("Evento inválido o versión no soportada");
    String prefix = e.path("eventType").asText().startsWith("Usuarios.") ? "/usuarios/" : "/roles/";
    if (!e.path("subject").asText().equals(prefix + e.path("data").path("entityId").asLong()))
      throw new IllegalArgumentException("Subject inconsistente");
    Instant.parse(e.path("eventTime").asText());
    return e;
  }
}
