package dataaccess;

import model.GameData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MemoryGameDAO implements GameDAO {
  private final Map<Integer, GameData> games = new HashMap<>();
  private int nextGameId = 1;

  @Override
  public void createGame(GameData game) throws DataAccessException {
    int gameId = nextGameId++;
    game = new GameData(gameId, game.whiteUsername(), game.blackUsername(), game.gameName(), game.game());
    games.put(gameId, game);
  }

  @Override
  public GameData getGame(int gameId) throws DataAccessException {
    return games.get(gameId);
  }

  @Override
  public Collection<GameData> listGames() throws DataAccessException {
    return games.values();
  }

  @Override
  public void updateGame(GameData game) throws DataAccessException {
    if (!games.containsKey(game.gameID())) {
      throw new DataAccessException("Error: game not found");
    }
    games.put(game.gameID(), game);
  }

  @Override
  public void clear() throws DataAccessException {
    games.clear();
    nextGameId = 1;
  }
}


