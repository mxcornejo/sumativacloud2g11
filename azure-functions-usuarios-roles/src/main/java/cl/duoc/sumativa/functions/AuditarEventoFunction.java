package cl.duoc.sumativa.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.*;

public class AuditarEventoFunction {
  @FunctionName("AuditarEvento")
  public void run(@EventGridTrigger(name="event") String raw, ExecutionContext context) throws Exception {
    JsonNode event = DomainEvent.validate(raw);
    String id = event.path("id").asText();
    try (Connection c = OracleConnection.open(); PreparedStatement insert = c.prepareStatement(
        "INSERT INTO auditoria_eventos(event_id,event_type,subject,occurred_at,payload) VALUES(?,?,?,?,?)")) {
      insert.setString(1,id); insert.setString(2,event.path("eventType").asText());
      insert.setString(3,event.path("subject").asText());
      insert.setTimestamp(4,Timestamp.from(java.time.Instant.parse(event.path("eventTime").asText())), java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")));
      insert.setString(5,DomainEvent.JSON.writeValueAsString(event.path("data")));
      try { insert.executeUpdate(); context.getLogger().info("EVENT_PROCESSED id=" + id); }
      catch (SQLException e) {
        if (e.getErrorCode() != 1) throw e;
        context.getLogger().info("EVENT_DUPLICATE_IGNORED id=" + id);
      }
    }
  }
}
