package dataAccess;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;
import java.util.*;

public class MemoryDataAccess implements DataAccess {
  private final Map<String, UserData> users = new HashMap<>();
  private final Map<Integer, GameData> games = new HashMap<>();
  private final Map<String, AuthData> auths = new HashMap<>();
  private int nextGameId = 1;

  @Override
  public void clear() {
    users.clear();
    games.clear();
    auths.clear();
    nextGameId = 1;
  }

  @Override
  public void createUser(UserData user) throws DataAccessException {
    if (users.containsKey(user.username())) {
      throw new DataAccessException("Username already exists");
    }
    users.put(user.username(), user);
  }

  @Override
  public UserData getUser(String username) {
    return users.get(username);
  }

  @Override
  public int createGame(String gameName) {
    int gameId = nextGameId++;
    games.put(gameId, new GameData(gameId, null, null, gameName, new ChessGame()));
    return gameId;
  }

  @Override
  public GameData getGame(int gameID) {
    return games.get(gameID);
  }

  @Override
  public List<GameData> listGames() {
    return new ArrayList<>(games.values());
  }

  @Override
  public void updateGame(GameData game) throws DataAccessException {
    if (!games.containsKey(game.gameID())) {
      throw new DataAccessException("Game not found");
    }
    games.put(game.gameID(), game);
  }

  @Override
  public String createAuth(String username) {
    String authToken = UUID.randomUUID().toString();
    auths.put(authToken, new AuthData(authToken, username));
    return authToken;
  }

  @Override
  public AuthData getAuth(String authToken) {
    return auths.get(authToken);
  }

  @Override
  public void deleteAuth(String authToken) {
    auths.remove(authToken);
  }
}