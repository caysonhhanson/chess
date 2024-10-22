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
    // Validate input parameters
    if (username == null || password == null || email == null ||
            username.isEmpty() || password.isEmpty() || email.isEmpty()) {
      throw new DataAccessException("Error: bad request");
    }

    // Check if user already exists
    UserData existingUser = dataAccess.getUser(username);
    if (existingUser != null) {
      throw new DataAccessException("Error: already taken");
    }

    // Create new user
    UserData newUser = new UserData(username, password, email);
    dataAccess.createUser(newUser);

    // Create and store auth token
    String authToken = UUID.randomUUID().toString();
    AuthData auth = new AuthData(username, authToken);
    dataAccess.createAuth(auth);

    return auth;
  }
  public AuthData login(String username, String password) throws DataAccessException {
    // Validate input parameters
    if (username == null || password == null ||
            username.isEmpty() || password.isEmpty()) {
      throw new DataAccessException("Error: bad request");
    }

    // Get the user
    UserData user = dataAccess.getUser(username);
    if (user == null || !user.password().equals(password)) {
      throw new DataAccessException("Error: unauthorized");
    }

    // Create and store new auth token
    String authToken = UUID.randomUUID().toString();
    AuthData auth = new AuthData(username, authToken);
    dataAccess.createAuth(auth);

    return auth;
  }
}
