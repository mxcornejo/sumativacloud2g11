package cl.duoc.sumativa.functions;

import com.microsoft.azure.functions.HttpStatus;

final class DomainErrors {
  private DomainErrors() {}

  static HttpStatus status(String code) {
    if (code.endsWith("_NOT_FOUND")) return HttpStatus.NOT_FOUND;
    if (code.startsWith("DUPLICATE_") || "ROLE_IN_USE".equals(code)) return HttpStatus.CONFLICT;
    if (code.startsWith("INVALID_") || "ID_REQUIRED".equals(code)) return HttpStatus.BAD_REQUEST;
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
