package cl.duoc.sumativa.functions;
import java.nio.file.*;
import java.sql.*;
/** Migración aditiva separada: soporta bloques PL/SQL delimitados por /. */
public class EventSchemaInitializer {
 public static void main(String[] args) throws Exception {
  String sql = Files.readString(Path.of(args[0])).replaceAll("(?m)^--.*$", "");
  try(Connection c=OracleConnection.open(); Statement s=c.createStatement()) {
   for(String command:sql.split("(?m)^/\\s*$")) {
    command=command.trim(); if(command.isEmpty()) continue;
    if(!command.startsWith("CREATE OR REPLACE TRIGGER")) command=command.replaceFirst(";\\s*$", "");
    s.execute(command);
   }
   try(ResultSet r=s.executeQuery("SELECT name,line,text FROM user_errors WHERE name IN ('TRG_USUARIOS_OUTBOX','TRG_ROLES_OUTBOX','TRG_USUARIO_ROL_VIGENTE','TRG_PROTEGER_ROL_DEFAULT')")) {
    if(r.next()) throw new SQLException("Error en trigger: " + r.getString(1) + " línea " + r.getInt(2) + " " + r.getString(3));
   }
  }
  System.out.println("Migración EDA aplicada y triggers compilados.");
 }
}
