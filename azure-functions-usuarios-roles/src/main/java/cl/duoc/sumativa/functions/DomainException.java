package cl.duoc.sumativa.functions;

final class DomainException extends RuntimeException {
  private final String code;

  DomainException(String code, String message) {
    super(message);
    this.code = code;
  }

  DomainException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  String code() {
    return code;
  }
}
