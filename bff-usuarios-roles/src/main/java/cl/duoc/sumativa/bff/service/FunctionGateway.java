package cl.duoc.sumativa.bff.service;

import cl.duoc.sumativa.bff.config.FunctionEndpointsProperties;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class FunctionGateway {
  private final RestTemplate restTemplate;
  private final FunctionEndpointsProperties endpoints;

  public FunctionGateway(RestTemplate restTemplate, FunctionEndpointsProperties endpoints) {
    this.restTemplate = restTemplate;
    this.endpoints = endpoints;
  }

  public ResponseEntity<String> forward(String domain, HttpMethod method, String resourceId, String body, String correlationId) {
    String url = functionUrl(domain, resourceId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.set("X-Correlation-Id", correlationId);
    String key = functionKey(domain);
    if (key != null && !key.isBlank()) {
      // Azure Functions accepts this header and keeps the secret out of URLs and logs.
      headers.set("x-functions-key", key);
    }
    try {
      ResponseEntity<String> response = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
      return ResponseEntity.status(response.getStatusCode()).contentType(MediaType.APPLICATION_JSON)
          .header("X-Correlation-Id", correlationId).body(response.getBody());
    } catch (HttpStatusCodeException exception) {
      return ResponseEntity.status(exception.getStatusCode()).contentType(MediaType.APPLICATION_JSON)
          .header("X-Correlation-Id", correlationId).body(exception.getResponseBodyAsString());
    } catch (ResourceAccessException exception) {
      throw new FunctionUnavailableException(domain);
    }
  }

  private String baseUrl(String domain) {
    return switch (domain) {
      case "users" -> endpoints.usersUrl();
      case "roles" -> endpoints.rolesUrl();
      default -> throw new IllegalArgumentException("Dominio no soportado");
    };
  }

  private String functionUrl(String domain, String resourceId) {
    String base = baseUrl(domain);
    int queryPosition = base.indexOf('?');
    String path = queryPosition < 0 ? base : base.substring(0, queryPosition);
    if (resourceId != null) path += "/" + resourceId;
    return path;
  }

  private String functionKey(String domain) {
    return "users".equals(domain) ? endpoints.usersKey() : endpoints.rolesKey();
  }
}
