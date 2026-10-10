package cl.duoc.sumativa.functions;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;

public class AuditarEventoFunction {
  @FunctionName("AuditarEvento")
  public void run(@EventGridTrigger(name="event") String raw, ExecutionContext context) throws Exception {
    JsonNode event = DomainEvent.validate(raw);
    try (Connection c = OracleConnection.open()) {
      boolean processed = EventProcessor.process(c, event);
      context.getLogger().info((processed ? "EVENT_PROCESSED id=" : "EVENT_DUPLICATE_IGNORED id=") + event.path("id").asText());
    }
  }
}
