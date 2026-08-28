package cl.duoc.sumativa.bff.service;

public class FunctionUnavailableException extends RuntimeException {
  public FunctionUnavailableException(String domain) {
    super("La función de " + domain + " no se encuentra disponible.");
  }
}
