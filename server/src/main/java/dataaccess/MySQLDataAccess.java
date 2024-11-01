package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.*;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class MySQLDataAccess implements DataAccess {
  private final Gson gson=new Gson();

  public MySQLDataAccess() {
    try {
      configureDatabase();
    } catch (DataAccessException e) {
      throw new RuntimeException("Failed to configure database: " + e.getMessage());
    }
  }

  private void configureDatabase() throws DataAccessException {
    DatabaseManager.createDatabase();
    try (Connection conn=DatabaseManager.getConnection()) {
      String[] createStatements={
              """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(255) PRIMARY KEY,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL
                )
                """,
              """
                CREATE TABLE IF NOT EXISTS games (
                    gameID INT PRIMARY KEY AUTO_INCREMENT,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255) NOT NULL,
                    gameState TEXT NOT NULL,
                    FOREIGN KEY (whiteUsername) REFERENCES users(username) ON DELETE SET NULL,
                    FOREIGN KEY (blackUsername) REFERENCES users(username) ON DELETE SET NULL
                )
                """,
              """
                CREATE TABLE IF NOT EXISTS auth_tokens (
                    authToken VARCHAR(255) PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
                )
                """
      };

      for (String statement : createStatements) {
        try (PreparedStatement ps=conn.prepareStatement(statement)) {
          ps.executeUpdate();
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void clear() throws DataAccessException {
    try (Connection conn = DatabaseManager.getConnection()) {
      String[] deleteStatements = {
              "DELETE FROM auth_tokens",
              "DELETE FROM games",
              "DELETE FROM users",
              "ALTER TABLE games AUTO_INCREMENT = 1"
      };

      for (String statement : deleteStatements) {
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
          ps.executeUpdate();
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void createUser(UserData user) throws DataAccessException {
    String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.username());
      ps.setString(2, BCrypt.hashpw(user.password(), BCrypt.gensalt())); // Use jBCrypt for hashing
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
  public void createGame(GameData game) throws DataAccessException {
    String sql = "INSERT INTO games (whiteUsername, blackUsername, gameName, gameState) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, game.whiteUsername());
      ps.setString(2, game.blackUsername());
      ps.setString(3, game.gameName());
      ps.setString(4, gson.toJson(game.game()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public GameData getGame(int gameId) throws BadRequestException {
    String sql = "SELECT * FROM games WHERE gameID = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, gameId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          ChessGame game = gson.fromJson(rs.getString("gameState"), ChessGame.class);
          return new GameData(
                  rs.getInt("gameID"),
                  rs.getString("whiteUsername"),
                  rs.getString("blackUsername"),
                  rs.getString("gameName"),
                  game
          );
        }
        return null;
      }
    } catch (SQLException | DataAccessException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @Override
  public Collection<GameData> listGames() throws DataAccessException {
    return null;
  }

  @Override
  public void updateGame(GameData game) throws DataAccessException {

  }

  @Override
  public void createAuth(AuthData auth) throws DataAccessException {

  }

  @Override
  public AuthData getAuth(String authToken) throws DataAccessException {
    return null;
  }

  @Override
  public void deleteAuth(String authToken) throws DataAccessException {

  }
}
