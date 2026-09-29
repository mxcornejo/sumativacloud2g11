package cl.duoc.sumativa.functions;
import java.sql.*;
import java.util.*;
/** Inspección de evidencia sin revelar datos personales o credenciales. */
public class EventEvidence {
 public static void main(String[] args) throws Exception {
  try(Connection c=OracleConnection.open();Statement s=c.createStatement()) {
   if(args.length>0 && args[0].equals("rollback")) {
    c.setAutoCommit(false);
    String name="EDA_ROLLBACK_"+System.currentTimeMillis();
    Map<String,Object> role=new RoleRepository(c).create(Map.of("name",name));
    long id=((Number)role.get("id")).longValue();
    String subject="/roles/"+id;
    try(PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM eventos_outbox WHERE subject=?")) {
     q.setString(1,subject);try(ResultSet r=q.executeQuery()){r.next();if(r.getInt(1)!=1)throw new AssertionError("Falta evento transaccional");}
     c.rollback();
     try(ResultSet r=q.executeQuery()){r.next();if(r.getInt(1)!=0)throw new AssertionError("Evento sobrevivió rollback");}
    }
    System.out.println("ROLLBACK_ATOMICITY_OK");return;
   }
   try(ResultSet r=s.executeQuery("SELECT o.event_id,o.event_type,o.subject,CASE WHEN o.published_at IS NULL THEN 'PENDING' ELSE 'PUBLISHED' END publication,CASE WHEN a.event_id IS NULL THEN 'PENDING' ELSE 'PROCESSED' END consumption FROM eventos_outbox o LEFT JOIN auditoria_eventos a ON a.event_id=o.event_id ORDER BY o.occurred_at DESC FETCH FIRST 30 ROWS ONLY")) {
    List<Map<String,String>> rows=new ArrayList<>();
    while(r.next()){Map<String,String> row=new LinkedHashMap<>();for(int n=1;n<=5;n++)row.put(r.getMetaData().getColumnLabel(n),r.getString(n));rows.add(row);}
    System.out.println(DomainEvent.JSON.writeValueAsString(rows));
   }
  }
 }
}
