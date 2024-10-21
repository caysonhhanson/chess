package dataaccess;

import model.GameData;
import java.util.List;

public interface GameDAO {
  void insertGame(GameData game) throws DataAccessException;
  GameData getGame(int gameID) throws DataAccessException;
  List<GameData> getAllGames() throws DataAccessException;
  void deleteGame(int gameID) throws DataAccessException;
}

