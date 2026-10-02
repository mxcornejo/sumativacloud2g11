import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.*;

/** Herramienta local Java de solo lectura; no despliega ni imprime secretos. */
public class VerAuditoria {
 public static void main(String[] args) {
  try {
   if(args.length != 2) throw new IllegalArgumentException("Uso: ./ver-auditoria.sh ID_ROL ID_USUARIO");
   long role=Long.parseLong(args[0]),user=Long.parseLong(args[1]);
   if(role<=0 || user<=0) throw new IllegalArgumentException("Los IDs deben ser positivos.");
   run(role,user);
  } catch(NumberFormatException e) {System.err.println("Los IDs deben ser números enteros.");System.exit(1);
  } catch(Exception e) {
   System.err.println(e instanceof IllegalArgumentException ? e.getMessage() :
    "No se pudo consultar Oracle. Comprueba conexión, sesión Azure, wallet y dependencias. No se muestran detalles sensibles.");
   System.exit(1);
  }
 }
 static void run(long role,long user) throws Exception {
  Path root=Path.of(System.getProperty("proyecto.root"));
  Path wallet=root.resolve("azure-functions-usuarios-roles/.oracle-wallet");
  if(!Files.exists(wallet.resolve("tnsnames.ora")))throw new IllegalArgumentException("Falta el wallet local del proyecto.");
  Path temp=Files.createTempDirectory(Path.of("/private/tmp"),"s3-audit-java-",
   PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
  try {
   Path output=Files.createFile(temp.resolve("config.json"),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
   Path errors=Files.createFile(temp.resolve("azure-errors.txt"),PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
   Process process=new ProcessBuilder("az","functionapp","config","appsettings","list",
    "-g","SUMATIVA1_USUARIOS_ROLES","-n","sumativa1usuariosrolesmc2026","-o","json","--only-show-errors")
    .redirectOutput(output.toFile()).redirectError(errors.toFile()).start();
   if(!process.waitFor(90,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor();throw new IllegalArgumentException("Azure no respondió a tiempo; repite la consulta.");}
   if(process.exitValue()!=0)throw new IllegalArgumentException("No se pudo leer Azure. Revisa tu sesión con az login antes de grabar.");
   JsonNode settings=new ObjectMapper().readTree(Files.readString(output));
   Map<String,String> config=new HashMap<>();
   for(JsonNode setting:settings)config.put(setting.path("name").asText(),setting.path("value").asText());
   Files.delete(output);Files.delete(errors);
   for(String key:List.of("ORACLE_DB_URL","ORACLE_DB_USER","ORACLE_DB_PASSWORD"))
    if(config.getOrDefault(key,"").isBlank())throw new IllegalArgumentException("Falta configuración de Oracle en Azure.");
   Path localWallet=Files.createDirectory(temp.resolve("wallet"));
   try(var files=Files.list(wallet)){for(Path p:files.filter(Files::isRegularFile).toList())Files.copy(p,localWallet.resolve(p.getFileName()));}
   System.setProperty("oracle.net.tns_admin",localWallet.toString());
   String url=config.get("ORACLE_DB_URL").replaceAll("([?&])TNS_ADMIN=[^&]*","$1").replaceAll("[?&]$","");
   url+=(url.contains("?")?"&":"?")+"TNS_ADMIN="+localWallet;
   Properties properties=new Properties();properties.setProperty("user",config.get("ORACLE_DB_USER"));
   properties.setProperty("password",config.get("ORACLE_DB_PASSWORD"));
   properties.setProperty("oracle.net.CONNECT_TIMEOUT","15000");properties.setProperty("oracle.jdbc.ReadTimeout","60000");
   try(Connection c=DriverManager.getConnection(url,properties);
       PreparedStatement q=c.prepareStatement("SELECT o.event_id,o.event_type,o.subject,CASE WHEN o.published_at IS NULL THEN 'PENDIENTE' ELSE 'PUBLICADO' END,CASE WHEN a.event_id IS NULL THEN 'PENDIENTE' ELSE 'PROCESADO' END FROM eventos_outbox o LEFT JOIN auditoria_eventos a ON a.event_id=o.event_id WHERE o.subject IN (?,?) ORDER BY o.occurred_at,o.event_id")) {
    q.setQueryTimeout(30);q.setString(1,"/roles/"+role);q.setString(2,"/usuarios/"+user);
    System.out.println("CONSULTA EN VIVO A ORACLE — "+OffsetDateTime.now().withNano(0));
    System.out.println("Rol: "+role+" | Usuario: "+user+" | Solo lectura");
    System.out.printf("%-34s %-19s %-17s %-12s %-12s%n","EVENT_ID","EVENT_TYPE","SUBJECT","PUBLICACION","CONSUMO");
    int total=0,done=0;
    try(ResultSet rows=q.executeQuery()){while(rows.next()){
     total++;if("PROCESADO".equals(rows.getString(5)))done++;
     System.out.printf("%-34s %-19s %-17s %-12s %-12s%n",rows.getString(1),rows.getString(2),rows.getString(3),rows.getString(4),rows.getString(5));
    }}
    System.out.println("Eventos encontrados: "+total+" | Procesados: "+done);
    if(total==0)System.out.println("No hay eventos para estos IDs. Comprueba las variables de Postman.");
    if(done<total)System.out.println("Hay pendientes. Repite después del próximo ciclo del publicador.");
   }
  } finally {
   try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
  }
 }
}
