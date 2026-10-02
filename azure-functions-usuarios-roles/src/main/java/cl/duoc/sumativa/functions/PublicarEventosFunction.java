package cl.duoc.sumativa.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Outbox transaccional: conserva el ID al reintentar; el consumidor deduplica. */
public class PublicarEventosFunction {
  @FunctionName("PublicarEventos")
  public void run(@TimerTrigger(name="timer", schedule="%OUTBOX_SCHEDULE%") String timer,
                  ExecutionContext context) throws Exception {
    try (Connection c = OracleConnection.open()) {
      List<String> ids = new ArrayList<>();
      try (PreparedStatement q = c.prepareStatement("SELECT event_id FROM eventos_outbox WHERE published_at IS NULL ORDER BY occurred_at FETCH FIRST 25 ROWS ONLY");
           ResultSet rows = q.executeQuery()) { while (rows.next()) ids.add(rows.getString(1)); }
      for (String id : ids) {
        c.setAutoCommit(false);
        try (PreparedStatement q = c.prepareStatement("SELECT event_type,subject,occurred_at,payload FROM eventos_outbox WHERE event_id=? AND published_at IS NULL FOR UPDATE SKIP LOCKED")) {
          q.setString(1, id);
          try (ResultSet row = q.executeQuery()) {
            if (row.next()) {
              String event = DomainEvent.encode(id, row.getString(1), row.getString(2),
                  row.getTimestamp(3, java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))).toInstant(), row.getString(4));
              EventGridPublisher.publish(event);
              try (PreparedStatement done = c.prepareStatement("UPDATE eventos_outbox SET published_at=SYS_EXTRACT_UTC(SYSTIMESTAMP) WHERE event_id=?")) {
                done.setString(1, id); done.executeUpdate();
              }
              context.getLogger().info("EVENT_PUBLISHED id=" + id);
            }
          }
          c.commit();
        } catch (Exception failure) {
          c.rollback();
          context.getLogger().warning("EVENT_PUBLISH_RETRY id=" + id);
          throw failure;
        }
      }
    }
  }
}
