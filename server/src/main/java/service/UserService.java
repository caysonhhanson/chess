package service;

import dataaccess.*;
import model.*;
import java.util.UUID;

public class UserService {
  private final DataAccess dataAccess;

  public UserService(DataAccess dataAccess) {
    this.dataAccess = dataAccess;
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
    AuthData auth = new AuthData(username, authToken);
    dataAccess.createAuth(auth);

    return auth;
  }

  public AuthData login(String username, String password) throws DataAccessException {
    if (username == null || password == null ||
            username.isEmpty() || password.isEmpty()) {
      throw new DataAccessException("Error: bad request");
    }

    UserData user=dataAccess.getUser(username);
    if (user == null || !user.password().equals(password)) {
      throw new DataAccessException("Error: unauthorized");
    }

    String authToken=UUID.randomUUID().toString();
    AuthData auth=new AuthData(username, authToken);
    dataAccess.createAuth(auth);

    return auth;
  }

  public void logout(AuthData authData) throws DataAccessException {
    AuthData existingAuth = dataAccess.getAuth(authData.authToken());
    if (existingAuth == null) {
      throw new DataAccessException("Error: unauthorized");
    }
    dataAccess.deleteAuth(authData.authToken());
  }

}