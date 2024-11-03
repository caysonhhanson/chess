package dataaccess;

import chess.ChessGame;
import model.GameData;
import com.google.gson.Gson;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class SQLGameDAO implements GameDAO {
  private final Gson gson = new Gson();

  @Override
  public void createGame(GameData game) throws DataAccessException {
    String sql = "INSERT INTO games (whiteUsername, blackUsername, gameName, gameState) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
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
  public GameData getGame(int gameId) throws DataAccessException {
    String sql = "SELECT * FROM games WHERE gameID = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, gameId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new GameData(
                  rs.getInt("gameID"),
                  rs.getString("whiteUsername"),
                  rs.getString("blackUsername"),
                  rs.getString("gameName"),
                  gson.fromJson(rs.getString("gameState"), ChessGame.class)
          );
        }
        return null;
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public Collection<GameData> listGames() throws DataAccessException {
    String sql = "SELECT * FROM games";
    Collection<GameData> games = new ArrayList<>();
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        games.add(new GameData(
                rs.getInt("gameID"),
                rs.getString("whiteUsername"),
                rs.getString("blackUsername"),
                rs.getString("gameName"),
                gson.fromJson(rs.getString("gameState"), ChessGame.class)
        ));
      }
      return games;
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void updateGame(GameData game) throws DataAccessException {
    String sql = "UPDATE games SET whiteUsername = ?, blackUsername = ?, gameName = ?, gameState = ? WHERE gameID = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, game.whiteUsername());
      ps.setString(2, game.blackUsername());
      ps.setString(3, game.gameName());
      ps.setString(4, gson.toJson(game.game()));
      ps.setInt(5, game.gameID());
      int rowsAffected = ps.executeUpdate();
      if (rowsAffected == 0) {
        throw new DataAccessException("Error: game not found");
      }
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }

  @Override
  public void clear() throws DataAccessException {
    String sql = "DELETE FROM games";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new DataAccessException(e.getMessage());
    }
  }
}


