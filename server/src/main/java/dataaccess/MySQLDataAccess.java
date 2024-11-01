package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.*;
import org.mindrot.jbcrypt.BCrypt; // Use jBCrypt instead of Spring's BCrypt
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
      // Create tables in correct order for foreign key constraints
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
}
