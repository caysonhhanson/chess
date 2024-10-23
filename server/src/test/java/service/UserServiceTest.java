package service;

import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
  private UserService userService;
  private DataAccess dataAccess;
  private AuthData validAuth;


  @BeforeEach
  public void setUp() {
    dataAccess = new MemoryDataAccess();
    userService = new UserService(dataAccess);
  }

  @Test
  public void registerSuccess() throws DataAccessException {
    String username = "testUser";
    String password = "password";
    String email = "email@test.com";

    AuthData result = userService.register(username, password, email);

    // First verify we got a valid auth result back
    assertNotNull(result);
    assertNotNull(result.authToken());
    assertEquals(username, result.username());

    // Then verify the user was created in the database
    UserData savedUser = dataAccess.getUser(username);
    assertNotNull(savedUser);
    assertEquals(username, savedUser.username());
    assertEquals(password, savedUser.password());
    assertEquals(email, savedUser.email());

    // Finally verify the auth token was stored correctly
    AuthData savedAuth = dataAccess.getAuth(result.authToken());
    assertNotNull(savedAuth);
    assertEquals(result.authToken(), savedAuth.authToken());
    assertEquals(username, savedAuth.username());
  }

  @Test
  public void registerFailDuplicate() throws DataAccessException {
    // First registration should succeed
    userService.register("testUser", "password", "email@test.com");

    // Second registration with same username should fail
    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "differentPassword", "different@test.com"));
  }

  @Test
  public void registerFailBadRequest() {
    // Test null username
    assertThrows(DataAccessException.class, () ->
            userService.register(null, "password", "email@test.com"));

    // Test empty username
    assertThrows(DataAccessException.class, () ->
            userService.register("", "password", "email@test.com"));

    // Test null password
    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", null, "email@test.com"));

    // Test empty password
    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "", "email@test.com"));

    // Test null email
    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "password", null));

    // Test empty email
    assertThrows(DataAccessException.class, () ->
            userService.register("testUser", "password", ""));
  }

  @Test
  public void loginSuccess() throws DataAccessException {
    // First register a user
    String username = "testUser";
    String password = "password";
    userService.register(username, password, "email@test.com");

    // Try to login
    AuthData result = userService.login(username, password);

    // Verify we got a valid auth result back
    assertNotNull(result);
    assertNotNull(result.authToken());
    assertEquals(username, result.username());

    // Verify the auth token was stored correctly
    AuthData savedAuth = dataAccess.getAuth(result.authToken());
    assertNotNull(savedAuth);
    assertEquals(result.authToken(), savedAuth.authToken());
    assertEquals(username, savedAuth.username());
  }

  @Test
  public void loginFailWrongPassword() throws DataAccessException {
    // First register a user
    String username = "testUser";
    userService.register(username, "correctPassword", "email@test.com");

    // Try to login with wrong password
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
            userService.login(username, "wrongPassword"));
    assertEquals("Error: unauthorized", exception.getMessage());
  }

  @Test
  public void loginFailUserDoesNotExist() {
    // Try to login with non-existent user
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
            userService.login("nonexistentUser", "password"));
    assertEquals("Error: unauthorized", exception.getMessage());
  }

  @Test
  public void loginFailBadRequest() {
    // Test null username
    assertThrows(DataAccessException.class, () ->
            userService.login(null, "password"));

    // Test empty username
    assertThrows(DataAccessException.class, () ->
            userService.login("", "password"));

    // Test null password
    assertThrows(DataAccessException.class, () ->
            userService.login("testUser", null));

    // Test empty password
    assertThrows(DataAccessException.class, () ->
            userService.login("testUser", ""));
  }
  @Test
  void logoutSuccess() throws DataAccessException {
    // First register a test user
    String username = "testUser";
    String password = "password";
    userService.register(username, password, "test@example.com");

    // Login to get a valid auth token
    AuthData validAuth = userService.login(username, password);

    // Test successful logout with valid auth token
    assertDoesNotThrow(() -> userService.logout(validAuth));

    // Verify the auth token is no longer valid by trying to use it again
    assertThrows(DataAccessException.class, () -> userService.logout(validAuth));
  }

  @Test
  void logoutFailInvalidAuth() {
    // Test with invalid auth token
    AuthData invalidAuth = new AuthData("invalidToken", "testUser");
    assertThrows(DataAccessException.class, () -> userService.logout(invalidAuth));
  }
}
