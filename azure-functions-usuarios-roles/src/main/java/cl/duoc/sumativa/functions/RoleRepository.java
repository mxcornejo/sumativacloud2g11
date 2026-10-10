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

final class RoleRepository {
  private final Connection connection;

  RoleRepository(Connection connection) {
    this.connection = connection;
  }

  List<Map<String, Object>> list() {
    try (Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT id,name,description,active FROM roles WHERE deleted_at IS NULL ORDER BY id")) {
      List<Map<String, Object>> roles = new ArrayList<>();
      while (result.next()) roles.add(map(result));
      return roles;
    } catch (SQLException exception) {
      throw databaseError(exception);
    }
  }

  Map<String, Object> find(long id) {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT id,name,description,active FROM roles WHERE id=? AND deleted_at IS NULL")) {
      statement.setLong(1, id);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? map(result) : null;
      }
    } catch (SQLException exception) {
      throw databaseError(exception);
    }
  }

  Map<String, Object> create(Map<String, Object> input) {
    String name = requiredText(input, "name");
    try (PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO roles(name,description,active) VALUES(?,?,?)", new String[]{"id"})) {
      statement.setString(1, name);
      statement.setString(2, optionalText(input, "description"));
      statement.setInt(3, active(input));
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (!keys.next()) throw new DomainException("DATABASE_ERROR", "No fue posible obtener el rol creado.");
        return find(keys.getLong(1));
      }
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 1) throw new DomainException("DUPLICATE_ROLE", "Ya existe un rol con ese nombre.");
      throw databaseError(exception);
    }
  }

  Map<String, Object> update(long id, Map<String, Object> input) {
    String name = requiredText(input, "name");
    try (PreparedStatement statement = connection.prepareStatement(
        "UPDATE roles SET name=?,description=?,active=?,updated_at=SYSTIMESTAMP WHERE id=? AND deleted_at IS NULL AND is_default=0")) {
      statement.setString(1, name);
      statement.setString(2, optionalText(input, "description"));
      statement.setInt(3, active(input));
      statement.setLong(4, id);
      if (statement.executeUpdate() == 0) {
        if (exists(id)) throw new DomainException("ROLE_IN_USE", "El rol por defecto está protegido.");
        throw new DomainException("ROLE_NOT_FOUND", "Rol no encontrado.");
      }
      return find(id);
    } catch (SQLException exception) {
      if (exception.getErrorCode() == 1) throw new DomainException("DUPLICATE_ROLE", "Ya existe un rol con ese nombre.");
      throw databaseError(exception);
    }
  }

  void delete(long id) {
    try (PreparedStatement statement = connection.prepareStatement(
        "UPDATE roles SET deleted_at=SYSTIMESTAMP,active=0,updated_at=SYSTIMESTAMP WHERE id=? AND deleted_at IS NULL AND is_default=0")) {
      statement.setLong(1, id);
      if (statement.executeUpdate() == 0) {
        if (exists(id)) throw new DomainException("ROLE_IN_USE", "El rol por defecto está protegido.");
        throw new DomainException("ROLE_NOT_FOUND", "Rol no encontrado.");
      }
    } catch (SQLException exception) {
      throw databaseError(exception);
    }
  }

  boolean exists(long id) {
    try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM roles WHERE id=? AND deleted_at IS NULL")) {
      statement.setLong(1, id);
      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    } catch (SQLException exception) {
      throw databaseError(exception);
    }
  }

  private Map<String, Object> map(ResultSet result) throws SQLException {
    Map<String, Object> role = new LinkedHashMap<>();
    role.put("id", result.getLong("id"));
    role.put("name", result.getString("name"));
    role.put("description", result.getString("description") == null ? "" : result.getString("description"));
    role.put("active", result.getInt("active") == 1);
    return role;
  }

  private static String requiredText(Map<String, Object> input, String field) {
    String value = optionalText(input, field);
    if (value == null) throw new DomainException("INVALID_REQUEST", field + " es obligatorio.");
    return value;
  }

  private static String optionalText(Map<String, Object> input, String field) {
    Object value = input.get(field);
    return value == null || value.toString().isBlank() ? null : value.toString().trim();
  }

  private static int active(Map<String, Object> input) {
    return Boolean.FALSE.equals(input.get("active")) ? 0 : 1;
  }

  private static DomainException databaseError(SQLException cause) {
    return new DomainException("DATABASE_ERROR", "No fue posible procesar la operación en Oracle.", cause);
  }
}
