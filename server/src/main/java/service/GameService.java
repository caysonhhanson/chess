package service;

import dataaccess.*;
import model.*;
import chess.ChessGame;
import java.util.Collection;

public class GameService {
  private final DataAccess dataAccess;

  public GameService(DataAccess dataAccess) {
    this.dataAccess = dataAccess;
  }

  public void clear() throws DataAccessException {
    dataAccess.clear();
  }

  public Collection<GameData> listGames(String authToken) throws DataAccessException, UnauthorizedException {
    AuthData auth = dataAccess.getAuth(authToken);
    if (auth == null) {
      throw new UnauthorizedException("Error: unauthorized");
    }

    return dataAccess.listGames();
  }
}