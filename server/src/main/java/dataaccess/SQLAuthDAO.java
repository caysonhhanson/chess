package dataaccess;

import model.AuthData;

import java.sql.*;

public class SQLAuthDAO implements AuthDAO {
  @Override
  public void createAuth(AuthData auth) throws DataAccessException {
    String sql = "INSERT INTO auth_tokens (authToken, username) VALUES (?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auth.authToken());
      ps.setString(2, auth.username());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public AuthData getAuth(String authToken) throws DataAccessException {
    String sql = "SELECT * FROM auth_tokens WHERE authToken = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, authToken);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new AuthData(
                  rs.getString("username"),
                  rs.getString("authToken")
          );
        }
        return null;
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void deleteAuth(String authToken) throws DataAccessException {
    String sql = "DELETE FROM auth_tokens WHERE authToken = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, authToken);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void clear() throws DataAccessException {
    String sql = "DELETE FROM auth_tokens";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }
}
