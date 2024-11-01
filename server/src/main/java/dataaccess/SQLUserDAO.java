package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class SQLUserDAO implements UserDAO {
  @Override
  public void createUser(UserData user) throws DataAccessException {
    String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.username());
      ps.setString(2, BCrypt.hashpw(user.password(), BCrypt.gensalt()));
      ps.setString(3, user.email());
      ps.executeUpdate();
    } catch (SQLException e) {
      if (e.getMessage().contains("Duplicate entry")) {
        throw new DataAccessException("Error: already taken");
      }
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public UserData getUser(String username) throws DataAccessException {
    String sql = "SELECT * FROM users WHERE username = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new UserData(
                  rs.getString("username"),
                  rs.getString("password"),
                  rs.getString("email")
          );
        }
        return null;
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public Collection<UserData> listUsers() throws DataAccessException {
    String sql = "SELECT * FROM users";
    Collection<UserData> users = new ArrayList<>();
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        users.add(new UserData(
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("email")
        ));
      }
      return users;
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void clear() throws DataAccessException {
    String sql = "DELETE FROM users";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }
}

