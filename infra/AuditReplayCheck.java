package cl.duoc.sumativa.functions;
import java.sql.*;
import com.microsoft.azure.functions.ExecutionContext;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;
public class AuditReplayCheck {
 public static void main(String[] args) throws Exception {
  try(Connection c=OracleConnection.open();PreparedStatement s=c.prepareStatement("SELECT event_id,event_type,subject,occurred_at,payload FROM eventos_outbox WHERE subject=? AND event_type='Roles.Created'")) {
   s.setString(1,args[0]);
   try(ResultSet r=s.executeQuery()) {
    if(!r.next())throw new AssertionError("Missing test event");
    String id=r.getString(1),raw=DomainEvent.encode(id,r.getString(2),r.getString(3),r.getTimestamp(4,java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))).toInstant(),r.getString(5));
    ExecutionContext ctx=(ExecutionContext)Proxy.newProxyInstance(ExecutionContext.class.getClassLoader(),new Class[]{ExecutionContext.class},(o,m,a)->m.getName().equals("getLogger")?Logger.getLogger("ReplayCheck"):null);
    new AuditarEventoFunction().run(raw,ctx);new AuditarEventoFunction().run(raw,ctx);
    try(PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM auditoria_eventos WHERE event_id=?")){q.setString(1,id);try(ResultSet rows=q.executeQuery()){rows.next();if(rows.getInt(1)!=1)throw new AssertionError("Duplicate persisted");}}
    System.out.println("IDEMPOTENCY_ORACLE_OK event_id="+id);
   }
  }
 }
}
