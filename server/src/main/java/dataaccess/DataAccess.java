// In server/src/main/java/dataAccess/DataAccess.java
package dataaccess;

import model.*;
import java.util.Collection;

public interface DataAccess {
  void clear() throws DataAccessException;
  void createUser(UserData user) throws DataAccessException;
  UserData getUser(String username) throws DataAccessException;
  void createGame(GameData game) throws DataAccessException;
  GameData getGame(int gameId) throws BadRequestException;
  Collection<GameData> listGames() throws DataAccessException;
  void updateGame(GameData game) throws DataAccessException;
  void createAuth(AuthData auth) throws DataAccessException;
  AuthData getAuth(String authToken) throws DataAccessException;
  void deleteAuth(String authToken) throws DataAccessException;
}