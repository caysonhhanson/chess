package dataaccess;

import model.*;
import java.util.List;

public interface DataAccess {
  void clear() throws DataAccessException;

  // User operations
  void createUser(UserData user) throws DataAccessException;
  UserData getUser(String username) throws DataAccessException;

  // Game operations
  int createGame(String gameName) throws DataAccessException;
  GameData getGame(int gameID) throws DataAccessException;
  List<GameData> listGames() throws DataAccessException;
  void updateGame(GameData game) throws DataAccessException;

  // Auth operations
  String createAuth(String username) throws DataAccessException;
  AuthData getAuth(String authToken) throws DataAccessException;
  void deleteAuth(String authToken) throws DataAccessException;
}