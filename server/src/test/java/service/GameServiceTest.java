package service;

import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
  private GameService service;
  private DataAccess dataAccess;

  @BeforeEach
  public void setUp() {
    dataAccess = new MemoryDataAccess();
    service = new GameService(dataAccess);
  }

  @Test
  public void clearSuccess() throws DataAccessException {
    // Register a user first
    service.register("testUser", "password", "email@test.com");

    // Clear the database
    service.clear();

    // Verify user no longer exists
    UserData user = dataAccess.getUser("testUser");
    assertNull(user);
  }

  @Test
  public void registerSuccess() throws DataAccessException {
    String username = "testUser";
    String password = "password";
    String email = "email@test.com";

    AuthData result = service.register(username, password, email);

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
    service.register("testUser", "password", "email@test.com");

    // Second registration with same username should fail
    assertThrows(DataAccessException.class, () ->
            service.register("testUser", "differentPassword", "different@test.com"));
  }

  @Test
  public void registerFailBadRequest() {
    // Test null username
    assertThrows(DataAccessException.class, () ->
            service.register(null, "password", "email@test.com"));

    // Test empty username
    assertThrows(DataAccessException.class, () ->
            service.register("", "password", "email@test.com"));

    // Test null password
    assertThrows(DataAccessException.class, () ->
            service.register("testUser", null, "email@test.com"));

    // Test empty password
    assertThrows(DataAccessException.class, () ->
            service.register("testUser", "", "email@test.com"));

    // Test null email
    assertThrows(DataAccessException.class, () ->
            service.register("testUser", "password", null));

    // Test empty email
    assertThrows(DataAccessException.class, () ->
            service.register("testUser", "password", ""));
  }

  @Test
  public void loginSuccess() throws DataAccessException {
    // First register a user
    String username = "testUser";
    String password = "password";
    service.register(username, password, "email@test.com");

    // Try to login
    AuthData result = service.login(username, password);

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
    service.register(username, "correctPassword", "email@test.com");

    // Try to login with wrong password
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
            service.login(username, "wrongPassword"));
    assertEquals("Error: unauthorized", exception.getMessage());
  }

  @Test
  public void loginFailUserDoesNotExist() {
    // Try to login with non-existent user
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
            service.login("nonexistentUser", "password"));
    assertEquals("Error: unauthorized", exception.getMessage());
  }

  @Test
  public void loginFailBadRequest() {
    // Test null username
    assertThrows(DataAccessException.class, () ->
            service.login(null, "password"));

    // Test empty username
    assertThrows(DataAccessException.class, () ->
            service.login("", "password"));

    // Test null password
    assertThrows(DataAccessException.class, () ->
            service.login("testUser", null));

    // Test empty password
    assertThrows(DataAccessException.class, () ->
            service.login("testUser", ""));
  }
}