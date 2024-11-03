package service;

import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
  private UserService userService;
  private UserDAO userDAO;
  private AuthDAO authDAO;

  @BeforeEach
  public void setUp() throws DataAccessException {
    DatabaseInitializer.initialize();

    userDAO = new SQLUserDAO();
    authDAO = new SQLAuthDAO();
    userService = new UserService(userDAO, authDAO);

    userDAO.clear();
    authDAO.clear();
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
    assertTrue(savedUser.password().startsWith("$2a"));
    assertNotEquals(password, savedUser.password());
    assertEquals(email, savedUser.email());

    AuthData savedAuth = authDAO.getAuth(result.authToken());
    assertNotNull(savedAuth);
    assertEquals(result.authToken(), savedAuth.authToken());
    assertEquals(username, savedAuth.username());
  }

  @Test
  public void registerFailDuplicate() throws DataAccessException {
    userService.register("testUser", "password", "email@test.com");

    DataAccessException exception = assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "differentPassword", "different@test.com")
    );
    assertEquals("Error: already taken", exception.getMessage());
  }

  @Test
  public void registerFailBadRequest() throws DataAccessException {
    assertThrows(DataAccessException.class, () ->
            userService.register(null, "password", "email@test.com")
    );
    assertThrows(DataAccessException.class, () ->
            userService.register("", "password", "email@test.com")
    );

    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", null, "email@test.com")
    );
    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "", "email@test.com")
    );

    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "password", null)
    );
    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "password", "")
    );

    Collection<UserData> users = userDAO.listUsers();
    assertTrue(users.isEmpty());
  }

  @Test
  void loginFailUserDoesNotExist() {
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
            userService.login("nonexistentUser", "password")
    );
    assertEquals("Error: unauthorized", exception.getMessage());
  }

  @Test
  void loginFailBadRequest() {
    assertThrows(DataAccessException.class, () ->
            userService.login(null, "password")
    );
    assertThrows(DataAccessException.class, () ->
            userService.login("", "password")
    );

    assertThrows(DataAccessException.class, () ->
            userService.login("testUser", null)
    );
    assertThrows(DataAccessException.class, () ->
            userService.login("testUser", "")
    );
  }

  @Test
  void logoutSuccess() throws DataAccessException {
    AuthData regResult = userService.register("testUser", "password", "test@example.com");
    assertDoesNotThrow(() -> userService.logout(regResult));

    assertNull(authDAO.getAuth(regResult.authToken()));

    assertThrows(DataAccessException.class, () -> userService.logout(regResult));
  }

  @Test
  void logoutFailInvalidAuth() {
    AuthData invalidAuth = new AuthData("testUser", "invalidToken");
    DataAccessException exception = assertThrows(DataAccessException.class,
            () -> userService.logout(invalidAuth)
    );
    assertEquals("Error: unauthorized", exception.getMessage());
  }
}
