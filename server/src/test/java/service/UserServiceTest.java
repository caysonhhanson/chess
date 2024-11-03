package service;

import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
  private UserService userService;
  private UserDAO userDAO;
  private AuthDAO authDAO;

  @BeforeEach
  public void setUp() {
    userDAO = new MemoryUserDAO();
    authDAO = new MemoryAuthDAO();
    userService = new UserService(userDAO, authDAO);
  }

  @Test
  public void registerSuccess() throws DataAccessException {
    String username = "testUser";
    String password = "password";
    String email = "email@test.com";

    AuthData result = userService.register(username, password, email);

    assertNotNull(result);
    assertNotNull(result.authToken());
    assertEquals(username, result.username());

    UserData savedUser = userDAO.getUser(username);
    assertNotNull(savedUser);
    assertEquals(username, savedUser.username());
    assertEquals(password, savedUser.password());
    assertEquals(email, savedUser.email());

    AuthData savedAuth = authDAO.getAuth(result.authToken());
    assertNotNull(savedAuth);
    assertEquals(result.authToken(), savedAuth.authToken());
    assertEquals(username, savedAuth.username());
  }

  @Test
  public void registerFailDuplicate() throws DataAccessException {
    userService.register("testUser", "password", "email@test.com");

    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "differentPassword", "different@test.com"));
  }

  @Test
  public void registerFailBadRequest() {
    assertThrows(DataAccessException.class, () ->
            userService.register(null, "password", "email@test.com"));

    assertThrows(DataAccessException.class, () ->
            userService.register("", "password", "email@test.com"));

    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", null, "email@test.com"));

    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "", "email@test.com"));

    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "password", null));

    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "password", ""));
  }

  @Test
  public void loginSuccess() throws DataAccessException {
    String username = "testUser";
    String password = "password";
    userService.register(username, password, "email@test.com");

    AuthData result = userService.login(username, password);

    assertNotNull(result);
    assertNotNull(result.authToken());
    assertEquals(username, result.username());

    AuthData savedAuth = authDAO.getAuth(result.authToken());
    assertNotNull(savedAuth);
    assertEquals(result.authToken(), savedAuth.authToken());
    assertEquals(username, savedAuth.username());
  }

  @Test
  public void loginFailWrongPassword() throws DataAccessException {
    String username = "testUser";
    userService.register(username, "correctPassword", "email@test.com");

    DataAccessException exception = assertThrows(DataAccessException.class, () ->
            userService.login(username, "wrongPassword"));
    assertEquals("Error: unauthorized", exception.getMessage());
  }

  @Test
  public void loginFailUserDoesNotExist() {
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
            userService.login("nonexistentUser", "password"));
    assertEquals("Error: unauthorized", exception.getMessage());
  }

  @Test
  public void loginFailBadRequest() {
    assertThrows(DataAccessException.class, () ->
            userService.login(null, "password"));

    assertThrows(DataAccessException.class, () ->
            userService.login("", "password"));

    assertThrows(DataAccessException.class, () ->
            userService.login("testUser", null));

    assertThrows(DataAccessException.class, () ->
            userService.login("testUser", ""));
  }

  @Test
  void logoutSuccess() throws DataAccessException {
    String username = "testUser";
    String password = "password";
    AuthData regResult = userService.register(username, password, "test@example.com");

    assertDoesNotThrow(() -> userService.logout(regResult));

    assertThrows(DataAccessException.class, () -> userService.logout(regResult));
  }

  @Test
  void logoutFailInvalidAuth() {
    // Test with invalid auth token
    AuthData invalidAuth = new AuthData("invalidToken", "testUser");
    assertThrows(DataAccessException.class, () -> userService.logout(invalidAuth));
  }
}
