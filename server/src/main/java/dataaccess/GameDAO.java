package dataaccess;

import model.GameData;
import java.util.Collection;

public interface GameDAO {
  void createGame(GameData game) throws DataAccessException;
  GameData getGame(int gameId) throws DataAccessException, BadRequestException;
  Collection<GameData> listGames() throws DataAccessException;

  void updateGame(GameData game) throws DataAccessException;
  void clear() throws DataAccessException;
}
