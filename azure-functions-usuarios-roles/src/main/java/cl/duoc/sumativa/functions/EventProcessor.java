package cl.duoc.sumativa.functions;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Business effects and deduplication commit atomically on a dedicated
 * connection.
 */
final class EventProcessor {
  static boolean process(Connection c, JsonNode event) throws Exception {
    if (!c.getAutoCommit())
      throw new IllegalArgumentException("Dedicated auto-commit connection required");
    c.setAutoCommit(false);
    try {
      try (PreparedStatement insert = c.prepareStatement(
          "INSERT INTO auditoria_eventos(event_id,event_type,subject,occurred_at,payload) VALUES(?,?,?,?,?)")) {
        insert.setString(1, event.path("id").asText());
        insert.setString(2, event.path("eventType").asText());
        insert.setString(3, event.path("subject").asText());
        insert.setTimestamp(4, Timestamp.from(Instant.parse(event.path("eventTime").asText())),
            Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        insert.setString(5, DomainEvent.JSON.writeValueAsString(event.path("data")));
        try {
          insert.executeUpdate();
        } catch (SQLException duplicate) {
          if (duplicate.getErrorCode() != 1 && !"23505".equals(duplicate.getSQLState()))
            throw duplicate;
          c.rollback();
          return false;
        }
      }
      long entityId = event.path("data").path("entityId").asLong();
      switch (event.path("eventType").asText()) {
        case "Usuarios.Created" -> {
          long defaultRole;
          try (Statement q = c.createStatement();
              ResultSet r = q.executeQuery(
                  "SELECT id FROM roles WHERE is_default=1 AND deleted_at IS NULL AND active=1")) {
            if (!r.next())
              throw new SQLException("Missing active default role");
            defaultRole = r.getLong(1);
          }
          try (PreparedStatement update = c.prepareStatement(
              "UPDATE usuarios SET role_id=?,default_pending=0,updated_at=SYSTIMESTAMP WHERE id=? AND role_id IS NULL AND default_pending=1")) {
            update.setLong(1, defaultRole);
            update.setLong(2, entityId);
            update.executeUpdate();
          }
        }
        case "Roles.Deleted" -> {
          // A stale or forged deletion cannot remove a live role from its users.
          try (PreparedStatement update = c.prepareStatement(
              "UPDATE usuarios SET role_id=NULL,default_pending=0,updated_at=SYSTIMESTAMP WHERE role_id=? AND EXISTS (SELECT 1 FROM roles WHERE id=? AND deleted_at IS NOT NULL)")) {
            update.setLong(1, entityId);
            update.setLong(2, entityId);
            update.executeUpdate();
          }
        }
        default -> {
          /* Other domain events retain their audit record. */ }
      }
      c.commit();
      return true;
    } catch (Exception failure) {
      c.rollback();
      throw failure;
    } finally {
      c.setAutoCommit(true);
    }
  }
}
