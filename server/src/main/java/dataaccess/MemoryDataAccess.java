package dataaccess;


import model.*;
import java.util.*;

public class MemoryDataAccess implements DataAccess {
  private final Map<String, UserData> users = new HashMap<>();
  private final Map<Integer, GameData> games = new HashMap<>();
  private final Map<String, AuthData> auths = new HashMap<>();
  private int nextGameId = 1;

  @Override
  public void clear() throws DataAccessException {
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
  public UserData getUser(String username) throws DataAccessException {
    return users.get(username);
  }

  @Override
  public void createGame(GameData game) throws DataAccessException {
    int gameId = nextGameId++;
    GameData newGame = new GameData(gameId, game.whiteUsername(), game.blackUsername(),
            game.gameName(), game.game());
    games.put(gameId, newGame);
  }

  @Override
  public GameData getGame(int gameId) {
    return games.get(gameId);
  }

  @Override
  public Collection<GameData> listGames() throws DataAccessException {
    return games.values();
  }

  @Override
  public void updateGame(GameData game) throws DataAccessException {
    if (!games.containsKey(game.gameID())) {
      throw new DataAccessException("Game not found");
    }
    games.put(game.gameID(), game);
  }

  @Override
  public void createAuth(AuthData auth) throws DataAccessException {
    auths.put(auth.authToken(), auth);
  }

  @Override
  public AuthData getAuth(String authToken) throws DataAccessException {
    return auths.get(authToken);
  }

  @Override
  public void deleteAuth(String authToken) throws DataAccessException {
    auths.remove(authToken);
  }
}