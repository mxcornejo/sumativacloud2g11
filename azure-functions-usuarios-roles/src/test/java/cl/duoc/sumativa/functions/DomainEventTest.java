package cl.duoc.sumativa.functions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
class DomainEventTest {
 private static final String ID="0123456789ABCDEF0123456789ABCDEF";
 @Test void allCrudTypesRoundTrip() throws Exception {
  for(String type:DomainEvent.TYPES) {
   String subject=(type.startsWith("Usuarios")?"/usuarios/":"/roles/")+42;
   String event=DomainEvent.encode(ID,type,subject,Instant.parse("2026-09-29T12:00:00Z"),"{\"entityId\":42}");
   assertEquals(ID,DomainEvent.validate(event).path("id").asText());
   assertEquals(type,DomainEvent.validate(event).path("eventType").asText());
  }
 }
 @Test void retriesPreserveIdAndPayload() throws Exception {
  Instant t=Instant.parse("2026-09-29T12:00:00Z");
  assertEquals(DomainEvent.encode(ID,"Roles.Created","/roles/1",t,"{\"entityId\":1}"),DomainEvent.encode(ID,"Roles.Created","/roles/1",t,"{\"entityId\":1}"));
 }
 @Test void inconsistentSubjectRejected() {
  assertThrows(IllegalArgumentException.class,()->DomainEvent.encode(ID,"Roles.Created","/usuarios/1",Instant.now(),"{\"entityId\":1}"));
 }
 @Test void unknownTypeRejected() {
  assertThrows(IllegalArgumentException.class,()->DomainEvent.encode(ID,"Roles.Unknown","/roles/1",Instant.now(),"{\"entityId\":1}"));
 }
 @Test void invalidIdentifierRejected() {
  assertThrows(IllegalArgumentException.class,()->DomainEvent.encode(ID,"Roles.Created","/roles/0",Instant.now(),"{\"entityId\":0}"));
 }
 @Test void malformedPayloadRejected() {
  assertThrows(Exception.class,()->DomainEvent.validate("{invalid"));
 }
}
