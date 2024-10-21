package service;

import dataaccess.*;
import model.*;
import chess.ChessGame;

import java.util.Collection;
import java.util.UUID;

public class GameService {
  private final DataAccess dataAccess;

  public GameService(DataAccess dataAccess) {
    this.dataAccess = dataAccess;
  }

  public void clear() throws DataAccessException {
    dataAccess.clear();
  }

  public AuthData register(String username, String password, String email) throws DataAccessException {
    if (username == null || password == null || email == null ||
            username.isEmpty() || password.isEmpty() || email.isEmpty()) {
      throw new DataAccessException("Error: bad request");
    }

    UserData existingUser = dataAccess.getUser(username);
    if (existingUser != null) {
      throw new DataAccessException("Error: already taken");
    }

    UserData newUser = new UserData(username, password, email);
    dataAccess.createUser(newUser);

    String authToken = UUID.randomUUID().toString();
    AuthData auth = new AuthData(authToken, username);
    dataAccess.createAuth(auth);

    return auth;
  }
}
