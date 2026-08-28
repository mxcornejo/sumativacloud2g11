package cl.duoc.sumativa.functions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.sql.*;
import java.util.*;

public class RolesFunction {
  @FunctionName("RolesFunction")
  public HttpResponseMessage run(@HttpTrigger(name="request", methods={HttpMethod.GET,HttpMethod.POST,HttpMethod.PUT,HttpMethod.DELETE}, authLevel=AuthorizationLevel.FUNCTION, route="roles/{id?}") HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
    String trace = request.getHeaders().getOrDefault("x-correlation-id", UUID.randomUUID().toString());
    String id = RouteParameter.id(request.getUri(), "roles");
    try (Connection connection = OracleConnection.open()) {
      return switch (request.getHttpMethod()) {
        case GET -> id == null ? JsonResponses.body(request, HttpStatus.OK, list(connection), trace) : one(request, connection, id, trace);
        case POST -> create(request, connection, trace);
        case PUT -> id == null ? JsonResponses.error(request, HttpStatus.BAD_REQUEST, "ID_REQUIRED", "El id del rol es obligatorio.", trace) : update(request, connection, id, trace);
        case DELETE -> id == null ? JsonResponses.error(request, HttpStatus.BAD_REQUEST, "ID_REQUIRED", "El id del rol es obligatorio.", trace) : delete(request, connection, id, trace);
        default -> JsonResponses.error(request, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Método no permitido.", trace);
      };
    } catch (IllegalStateException exception) { context.getLogger().severe(exception.getMessage()); return JsonResponses.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "CONFIGURATION_ERROR", "Configuración de base de datos incompleta.", trace); }
      catch (SQLException exception) { context.getLogger().severe(exception.getMessage()); return JsonResponses.error(request, HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "No fue posible procesar la operación.", trace); }
  }
  private HttpResponseMessage create(HttpRequestMessage<Optional<String>> req, Connection c, String trace) {
    try { Map<String,Object> body = JsonResponses.JSON.readValue(req.getBody().orElseThrow(), new TypeReference<>(){}); String name = text(body,"name"); if(name == null) return JsonResponses.error(req,HttpStatus.BAD_REQUEST,"INVALID_REQUEST","name es obligatorio.",trace); try (PreparedStatement ps=c.prepareStatement("INSERT INTO roles(name,description,active) VALUES(?,?,?)", new String[]{"id"})) { ps.setString(1,name); ps.setString(2,(String)body.get("description")); ps.setInt(3, active(body)); ps.executeUpdate(); ResultSet keys=ps.getGeneratedKeys(); keys.next(); return JsonResponses.body(req,HttpStatus.CREATED,role(c,keys.getLong(1)),trace); } } catch(Exception e){return JsonResponses.error(req,HttpStatus.BAD_REQUEST,"INVALID_REQUEST","JSON inválido o datos incorrectos.",trace);} }
  private HttpResponseMessage update(HttpRequestMessage<Optional<String>> req, Connection c, String id, String trace) { try { Map<String,Object> body=JsonResponses.JSON.readValue(req.getBody().orElseThrow(),new TypeReference<>(){}); String name=text(body,"name"); if(name==null)return JsonResponses.error(req,HttpStatus.BAD_REQUEST,"INVALID_REQUEST","name es obligatorio.",trace); try(PreparedStatement ps=c.prepareStatement("UPDATE roles SET name=?,description=?,active=?,updated_at=SYSTIMESTAMP WHERE id=?")){ps.setString(1,name);ps.setString(2,(String)body.get("description"));ps.setInt(3,active(body));ps.setLong(4,Long.parseLong(id)); if(ps.executeUpdate()==0)return JsonResponses.error(req,HttpStatus.NOT_FOUND,"ROLE_NOT_FOUND","Rol no encontrado.",trace);return JsonResponses.body(req,HttpStatus.OK,role(c,Long.parseLong(id)),trace);}}catch(Exception e){return JsonResponses.error(req,HttpStatus.BAD_REQUEST,"INVALID_REQUEST","JSON o id inválido.",trace);} }
  private HttpResponseMessage delete(HttpRequestMessage<?> req, Connection c, String id, String trace) { try { try(PreparedStatement check=c.prepareStatement("SELECT COUNT(*) FROM usuarios WHERE role_id=?")){check.setLong(1,Long.parseLong(id));ResultSet rs=check.executeQuery();rs.next();if(rs.getInt(1)>0)return JsonResponses.error(req,HttpStatus.CONFLICT,"ROLE_IN_USE","No se puede eliminar un rol con usuarios asociados.",trace);} try(PreparedStatement ps=c.prepareStatement("DELETE FROM roles WHERE id=?")){ps.setLong(1,Long.parseLong(id));if(ps.executeUpdate()==0)return JsonResponses.error(req,HttpStatus.NOT_FOUND,"ROLE_NOT_FOUND","Rol no encontrado.",trace);return req.createResponseBuilder(HttpStatus.NO_CONTENT).header("X-Correlation-Id",trace).build();}}catch(Exception e){return JsonResponses.error(req,HttpStatus.BAD_REQUEST,"INVALID_ID","Id inválido.",trace);} }
  private HttpResponseMessage one(HttpRequestMessage<?> req, Connection c, String id, String trace)throws SQLException { try{Map<String,Object> value=role(c,Long.parseLong(id));return value==null?JsonResponses.error(req,HttpStatus.NOT_FOUND,"ROLE_NOT_FOUND","Rol no encontrado.",trace):JsonResponses.body(req,HttpStatus.OK,value,trace);}catch(NumberFormatException e){return JsonResponses.error(req,HttpStatus.BAD_REQUEST,"INVALID_ID","Id inválido.",trace);} }
  private List<Map<String,Object>> list(Connection c)throws SQLException {List<Map<String,Object>> values=new ArrayList<>();try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT id,name,description,active FROM roles ORDER BY id")){while(rs.next())values.add(map(rs));}return values;}
  private Map<String,Object> role(Connection c,long id)throws SQLException {try(PreparedStatement ps=c.prepareStatement("SELECT id,name,description,active FROM roles WHERE id=?")){ps.setLong(1,id);ResultSet rs=ps.executeQuery();return rs.next()?map(rs):null;}}
  private Map<String,Object> map(ResultSet rs)throws SQLException{return Map.of("id",rs.getLong("id"),"name",rs.getString("name"),"description",Optional.ofNullable(rs.getString("description")).orElse(""),"active",rs.getInt("active")==1);}
  private String text(Map<String,Object> data,String field){Object value=data.get(field);return value==null||value.toString().isBlank()?null:value.toString();}
  private int active(Map<String,Object> data){return Boolean.FALSE.equals(data.get("active"))?0:1;}
}
