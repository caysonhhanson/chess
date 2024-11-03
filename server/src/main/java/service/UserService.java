package service;

import dataaccess.*;
import model.*;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class UserService {
  private final UserDAO userDAO;
  private final AuthDAO authDAO;

  public UserService(UserDAO userDAO, AuthDAO authDAO) {
    this.userDAO = userDAO;
    this.authDAO = authDAO;
  }

  public AuthData register(String username, String password, String email) throws DataAccessException {
    if (username == null || password == null || email == null ||
            username.isEmpty() || password.isEmpty() || email.isEmpty()) {
      throw new DataAccessException("Error: bad request");
    }

    UserData existingUser = userDAO.getUser(username);
    if (existingUser != null) {
      throw new DataAccessException("Error: already taken");
    }

    // Let the DAO handle password hashing
    UserData newUser = new UserData(username, password, email);
    userDAO.createUser(newUser);

    String authToken = UUID.randomUUID().toString();
    AuthData auth = new AuthData(username, authToken);
    authDAO.createAuth(auth);

    return auth;
  }

  public AuthData login(String username, String password) throws DataAccessException {
    if (username == null || password == null ||
            username.isEmpty() || password.isEmpty()) {
      throw new DataAccessException("Error: bad request");
    }

    UserData user = userDAO.getUser(username);
    if (user == null) {
      throw new DataAccessException("Error: unauthorized");
    }

    // Get stored user and compare passwords, let DAO handle BCrypt verification
    UserData storedUser = userDAO.getUser(username);
    if (storedUser == null || !BCrypt.checkpw(password, storedUser.password())) {
      throw new DataAccessException("Error: unauthorized");
    }

    String authToken = UUID.randomUUID().toString();
    AuthData auth = new AuthData(username, authToken);
    authDAO.createAuth(auth);

    return auth;
  }

  public void logout(AuthData authData) throws DataAccessException {
    AuthData existingAuth = authDAO.getAuth(authData.authToken());
    if (existingAuth == null) {
      throw new DataAccessException("Error: unauthorized");
    }
    authDAO.deleteAuth(authData.authToken());
  }
}