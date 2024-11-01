package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;

import java.sql.SQLException;
import java.util.HashSet;

public class SQLGameDAO implements GameDAO {

  public SQLGameDAO() {
    try {
      DatabaseManager.createDatabase();
    } catch (DataAccessException ex) {
      throw new RuntimeException(ex);
    }
    try (var conn = DatabaseManager.getConnection()) {
      var createTestTable = """
                CREATE TABLE if NOT EXISTS game (
                    gameID INT NOT NULL,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255),
                    chessGame TEXT,
                    PRIMARY KEY (gameID)
                )""";
      try (var preparedStatement = conn.prepareStatement(createTestTable)) {
        preparedStatement.executeUpdate();
      }
    } catch (SQLException | DataAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public HashSet<GameData> listGames() {
    HashSet<GameData> games = new HashSet<>(16);
    try (var conn = DatabaseManager.getConnection()) {
      try (var preparedStatement = conn.prepareStatement("SELECT gameID, whiteUsername, blackUsername, gameName, chessGame FROM game")) {
        try (var results = preparedStatement.executeQuery()) {
          while (results.next()) {
            var gameID = results.getInt("gameID");
            var whiteUsername = results.getString("whiteUsername");
            var blackUsername = results.getString("blackUsername");
            var gameName = results.getString("gameName");
            var chessGame = new Gson().fromJson(results.getString("chessGame"), ChessGame.class);
            games.add(new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame));
          }
        }
      }
    } catch (SQLException | DataAccessException e) {
      return null;
    }
    return games;
  }

  @Override
  public void createGame(GameData game) throws DataAccessException {
    try (var conn = DatabaseManager.getConnection()) {
      try (var preparedStatement = conn.prepareStatement("INSERT INTO game (gameID, whiteUsername, blackUsername, gameName, chessGame) VALUES(?, ?, ?, ?, ?)")) {
        preparedStatement.setInt(1, game.gameID());
        preparedStatement.setString(2, game.whiteUsername());
        preparedStatement.setString(3, game.blackUsername());
        preparedStatement.setString(4, game.gameName());
        preparedStatement.setString(5, new Gson().toJson(game.game()));
        preparedStatement.executeUpdate();
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public GameData getGame(int gameID) throws DataAccessException, BadRequestException {
    try (var conn = DatabaseManager.getConnection()) {
      try (var preparedStatement = conn.prepareStatement("SELECT whiteUsername, blackUsername, gameName, chessGame FROM game WHERE gameID=?")) {
        preparedStatement.setInt(1, gameID);
        try (var results = preparedStatement.executeQuery()) {
          if (results.next()) {
            var whiteUsername = results.getString("whiteUsername");
            var blackUsername = results.getString("blackUsername");
            var gameName = results.getString("gameName");
            var chessGame = new Gson().fromJson(results.getString("chessGame"), ChessGame.class);
            return new GameData(gameID, whiteUsername, blackUsername, gameName, chessGame);
          } else {
            throw new BadRequestException("Game not found, id: " + gameID);
          }
        }
      }
    } catch (SQLException e) {
      throw new BadRequestException("Game not found, id: " + gameID);
    }
  }

  @Override
  public void updateGame(GameData game) throws DataAccessException {
    try (var conn = DatabaseManager.getConnection()) {
      try (var preparedStatement = conn.prepareStatement("UPDATE game SET whiteUsername=?, blackUsername=?, gameName=?, chessGame=? WHERE gameID=?")) {
        preparedStatement.setString(1, game.whiteUsername());
        preparedStatement.setString(2, game.blackUsername());
        preparedStatement.setString(3, game.gameName());
        preparedStatement.setString(4, new Gson().toJson(game.game()));
        preparedStatement.setInt(5, game.gameID());
        int rowsUpdated = preparedStatement.executeUpdate();
        if (rowsUpdated == 0) throw new DataAccessException("Item requested to be updated not found");
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void clear() {
    try (var conn = DatabaseManager.getConnection()) {
      try (var preparedStatement = conn.prepareStatement("TRUNCATE game")) {
        preparedStatement.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    } catch (SQLException | DataAccessException e) {
    }
  }
}

