package cl.duoc.sumativa.bff.api;

import cl.duoc.sumativa.bff.service.FunctionUnavailableException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(FunctionUnavailableException.class)
  ResponseEntity<ApiError> unavailable(FunctionUnavailableException exception) {
    String id = UUID.randomUUID().toString();
    return ResponseEntity.status(503).header("X-Correlation-Id", id)
        .body(ApiError.of(503, "FUNCTION_UNAVAILABLE", exception.getMessage(), id));
  }
}
