package cl.duoc.sumativa.functions;

import java.net.URI;

/** Extrae el id opcional desde una ruta HTTP sin exigir un binding inexistente. */
final class RouteParameter {
  private RouteParameter() {}
  static String id(URI uri, String resource) {
    String normalized = uri.getPath().replaceAll("/+", "/");
    String marker = "/" + resource + "/";
    int position = normalized.indexOf(marker);
    if (position < 0) return null;
    String value = normalized.substring(position + marker.length());
    return value.isBlank() || value.contains("/") ? null : value;
  }
}
