package cl.duoc.sumativa.functions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/** Ejecuta el esquema de la Sumativa usando el Wallet y variables de entorno, sin almacenar credenciales. */
public final class SchemaInitializer {
  private SchemaInitializer() {}
  public static void main(String[] args) throws Exception {
    String scriptFile = System.getenv().getOrDefault("ORACLE_SCHEMA_FILE", "../diseno/oracle-schema.sql");
    String sql = Files.readString(Path.of(scriptFile)).replaceAll("(?m)^--.*$", "");
    try (Connection connection = OracleConnection.open(); Statement statement = connection.createStatement()) {
      for (String command : sql.split(";")) if (!command.isBlank()) statement.execute(command);
    }
    System.out.println("Esquema Oracle creado correctamente.");
  }
}
