package cl.duoc.sumativa.functions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class OracleConnection {
  private OracleConnection() {}
  static Connection open() throws SQLException {
    String walletPath = walletPath();
    System.setProperty("oracle.net.tns_admin", walletPath);
    String url = required("ORACLE_DB_URL");
    if (!url.contains("TNS_ADMIN=")) url += (url.contains("?") ? "&" : "?") + "TNS_ADMIN=" + walletPath;
    return DriverManager.getConnection(url, required("ORACLE_DB_USER"), required("ORACLE_DB_PASSWORD"));
  }
  private static String walletPath() throws SQLException {
    String configured = System.getenv("ORACLE_WALLET_PATH");
    if (configured != null && Files.exists(Path.of(configured, "tnsnames.ora"))) return configured;
    try {
      Path directory = Path.of(System.getProperty("java.io.tmpdir"), "sumativa-oracle-wallet");
      Files.createDirectories(directory);
      for (String filename : new String[]{"sqlnet.ora", "tnsnames.ora", "ewallet.pem", "keystore.jks", "cwallet.sso", "ewallet.p12", "truststore.jks", "ojdbc.properties"}) {
        Path target = directory.resolve(filename);
        if (Files.exists(target)) continue;
        try (InputStream input = OracleConnection.class.getResourceAsStream("/.oracle-wallet/" + filename)) {
          if (input == null) throw new IOException("No se encontró " + filename);
          Files.copy(input, target);
        }
      }
      return directory.toString();
    } catch (IOException exception) { throw new SQLException("No se pudo preparar el Wallet de Oracle", exception); }
  }
  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalStateException("Falta configurar " + name);
    return value;
  }
}
