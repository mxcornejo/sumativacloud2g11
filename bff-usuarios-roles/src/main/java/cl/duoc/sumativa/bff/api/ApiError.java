package cl.duoc.sumativa.bff.api;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String code, String message, String correlationId) {
  static ApiError of(int status, String code, String message, String correlationId) {
    return new ApiError(Instant.now(), status, code, message, correlationId);
  }
}
