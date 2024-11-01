package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class SQLGameDAO implements GameDAO {
  private final Gson gson = new Gson();

  public SQLGameDAO() {
    try {
      DatabaseManager.createDatabase(); // Ensure the database is created
    } catch (DataAccessException ex) {
      throw new RuntimeException(ex);
    }
    try (Connection conn = DatabaseManager.getConnection()) {
      var createTableSQL = """
                CREATE TABLE IF NOT EXISTS games (
                    gameID INT NOT NULL,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255),
                    gameState TEXT,
                    PRIMARY KEY (gameID)
                )
            """;
      try (var createTableStatement = conn.prepareStatement(createTableSQL)) {
        createTableStatement.executeUpdate();
      }
    } catch (SQLException | DataAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<GameData> listGames() throws DataAccessException {
    Collection<GameData> games = new ArrayList<>();
    try (Connection conn = DatabaseManager.getConnection()) {
      var sql = "SELECT gameID, whiteUsername, blackUsername, gameName, gameState FROM games";
      try (var statement = conn.prepareStatement(sql);
           var results = statement.executeQuery()) {
        while (results.next()) {
          int gameID = results.getInt("gameID");
          String whiteUsername = results.getString("whiteUsername");
          String blackUsername = results.getString("blackUsername");
          String gameName = results.getString("gameName");
          ChessGame chessGame = deserializeGame(results.getString("gameState"));
          games.add(new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame));
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
    return games;
  }

  @Override
  public void createGame(GameData game) throws DataAccessException {
    String sql = "INSERT INTO games (gameID, whiteUsername, blackUsername, gameName, gameState) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         var statement = conn.prepareStatement(sql)) {
      statement.setInt(1, game.gameID());
      statement.setString(2, game.whiteUsername());
      statement.setString(3, game.blackUsername());
      statement.setString(4, game.gameName());
      statement.setString(5, serializeGame(game.game()));
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public GameData getGame(int gameID) throws DataAccessException {
    String sql = "SELECT whiteUsername, blackUsername, gameName, gameState FROM games WHERE gameID=?";
    try (Connection conn = DatabaseManager.getConnection();
         var statement = conn.prepareStatement(sql)) {
      statement.setInt(1, gameID);
      try (var results = statement.executeQuery()) {
        if (results.next()) {
          String whiteUsername = results.getString("whiteUsername");
          String blackUsername = results.getString("blackUsername");
          String gameName = results.getString("gameName");
          ChessGame chessGame = deserializeGame(results.getString("gameState"));
          return new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame);
        } else {
          throw new DataAccessException("Game not found, id: " + gameID);
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void updateGame(GameData game) throws DataAccessException {
    String sql = "UPDATE games SET whiteUsername=?, blackUsername=?, gameName=?, gameState=? WHERE gameID=?";
    try (Connection conn = DatabaseManager.getConnection();
         var statement = conn.prepareStatement(sql)) {
      statement.setString(1, game.whiteUsername());
      statement.setString(2, game.blackUsername());
      statement.setString(3, game.gameName());
      statement.setString(4, serializeGame(game.game()));
      statement.setInt(5, game.gameID());
      int rowsUpdated = statement.executeUpdate();
      if (rowsUpdated == 0) {
        throw new DataAccessException("Item requested to be updated not found");
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void clear() throws DataAccessException {
    try (Connection conn = DatabaseManager.getConnection();
         var statement = conn.prepareStatement("TRUNCATE games")) {
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  private String serializeGame(ChessGame game) {
    return gson.toJson(game);
  }

  private ChessGame deserializeGame(String serializedGame) {
    return gson.fromJson(serializedGame, ChessGame.class);
  }
}
