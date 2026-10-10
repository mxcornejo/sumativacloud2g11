package cl.duoc.sumativa.functions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class UserRepository {
  private final Connection connection;
  private final RoleRepository roles;

  UserRepository(Connection connection) {
    this.connection = connection;
    this.roles = new RoleRepository(connection);
  }

  List<Map<String, Object>> list() {
    try (Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT id,full_name,email,role_id,active FROM usuarios ORDER BY id")) {
      List<Map<String, Object>> users = new ArrayList<>();
      while (result.next()) users.add(map(result));
      return users;
    } catch (SQLException exception) {
      throw databaseError(exception);
    }
  }

  Map<String, Object> find(long id) {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT id,full_name,email,role_id,active FROM usuarios WHERE id=?")) {
      statement.setLong(1, id);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? map(result) : null;
      }
    } catch (SQLException exception) {
      throw databaseError(exception);
    }
  }

  Map<String, Object> create(Map<String, Object> input) {
    String fullName = requiredText(input, "fullName");
    String email = requiredText(input, "email");
    if (input.get("roleId") != null) throw new DomainException("INVALID_REQUEST", "Al crear se asigna el rol por defecto mediante eventos; cambie el rol con PUT después.");
    Long roleId = null;
    try (PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO usuarios(full_name,email,role_id,active,default_pending) VALUES(?,?,?,?,?)", new String[]{"id"})) {
      statement.setString(1, fullName);
      statement.setString(2, email);
      if (roleId == null) statement.setNull(3, java.sql.Types.NUMERIC);
      else statement.setLong(3, roleId);
      statement.setInt(4, active(input));
      statement.setInt(5, roleId == null ? 1 : 0);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (!keys.next()) throw new DomainException("DATABASE_ERROR", "No fue posible obtener el usuario creado.");
        return find(keys.getLong(1));
      }
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 1) throw new DomainException("DUPLICATE_USER", "Ya existe un usuario con ese correo.");
      throw databaseError(exception);
    }
  }

  Map<String, Object> update(long id, Map<String, Object> input) {
    String fullName = requiredText(input, "fullName");
    String email = requiredText(input, "email");
    Long roleId = input.get("roleId") == null ? null : requiredId(input, "roleId");
    if (roleId != null) requireRole(roleId);
    try (PreparedStatement statement = connection.prepareStatement(
        "UPDATE usuarios SET full_name=?,email=?,role_id=?,active=?,default_pending=0,updated_at=SYSTIMESTAMP WHERE id=?")) {
      statement.setString(1, fullName);
      statement.setString(2, email);
      if (roleId == null) statement.setNull(3, java.sql.Types.NUMERIC);
      else statement.setLong(3, roleId);
      statement.setInt(4, active(input));
      statement.setLong(5, id);
      if (statement.executeUpdate() == 0) throw new DomainException("USER_NOT_FOUND", "Usuario no encontrado.");
      return find(id);
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 1) throw new DomainException("DUPLICATE_USER", "Ya existe un usuario con ese correo.");
      throw databaseError(exception);
    }
  }

  void delete(long id) {
    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM usuarios WHERE id=?")) {
      statement.setLong(1, id);
      if (statement.executeUpdate() == 0) throw new DomainException("USER_NOT_FOUND", "Usuario no encontrado.");
    } catch (SQLException exception) {
      throw databaseError(exception);
    }
  }

  private void requireRole(long roleId) {
    if (!roles.exists(roleId)) throw new DomainException("ROLE_NOT_FOUND", "El rol indicado no existe.");
  }

  private Map<String, Object> map(ResultSet result) throws SQLException {
    Map<String, Object> user = new LinkedHashMap<>();
    user.put("id", result.getLong("id"));
    user.put("fullName", result.getString("full_name"));
    user.put("email", result.getString("email"));
    long roleId = result.getLong("role_id");
    user.put("roleId", result.wasNull() ? null : roleId);
    user.put("active", result.getInt("active") == 1);
    return user;
  }

  private static String requiredText(Map<String, Object> input, String field) {
    Object value = input.get(field);
    if (value == null || value.toString().isBlank()) throw new DomainException("INVALID_REQUEST", field + " es obligatorio.");
    return value.toString().trim();
  }

  private static long requiredId(Map<String, Object> input, String field) {
    try {
      Object value = input.get(field);
      if (value == null) throw new NumberFormatException();
      long id = Long.parseLong(value.toString());
      if (id <= 0) throw new NumberFormatException();
      return id;
    } catch (NumberFormatException exception) {
      throw new DomainException("INVALID_REQUEST", field + " debe ser un identificador válido.");
    }
  }

  private static int active(Map<String, Object> input) {
    return Boolean.FALSE.equals(input.get("active")) ? 0 : 1;
  }

  private static DomainException databaseError(SQLException cause) {
    if (cause.getErrorCode() == 20001 || cause.getErrorCode() == 2291) return new DomainException("ROLE_NOT_FOUND", "El rol indicado no está disponible.");
    return new DomainException("DATABASE_ERROR", "No fue posible procesar la operación en Oracle.", cause);
  }
}
