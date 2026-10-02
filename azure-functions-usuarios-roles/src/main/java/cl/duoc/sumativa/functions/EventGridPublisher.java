package cl.duoc.sumativa.functions;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

final class EventGridPublisher {
  private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  static void publish(String event) throws Exception {
    URI endpoint = URI.create(required("EVENT_GRID_ENDPOINT"));
    if (!"https".equals(endpoint.getScheme()) || endpoint.getHost() == null || !endpoint.getHost().endsWith(".eventgrid.azure.net"))
      throw new IllegalStateException("Endpoint Event Grid no válido");
    HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(20))
        .header("aeg-sas-key", required("EVENT_GRID_KEY")).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("[" + event + "]")).build();
    int status = CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    if (status < 200 || status >= 300) throw new IllegalStateException("Publicación Event Grid HTTP " + status);
  }
  private static String required(String key) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) throw new IllegalStateException("Falta " + key);
    return value;
  }
}
