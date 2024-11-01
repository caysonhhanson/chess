package dataaccess;

import chess.ChessGame;
import model.*;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLDataAccessTest {
  private MySQLDataAccess dataAccess;
  private static final String TEST_USERNAME = "testUser";
  private static final String TEST_PASSWORD = "testPass";
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_AUTH_TOKEN = "testAuthToken";
  private static final String TEST_GAME_NAME = "testGame";

  @BeforeEach
  public void setUp() throws DataAccessException {
    dataAccess = new MySQLDataAccess();
    dataAccess.clear(); // Start with a clean database
  }

  // User Tests
  @Test
  public void createUser_success() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    dataAccess.createUser(user);

    UserData retrievedUser = dataAccess.getUser(TEST_USERNAME);
    assertNotNull(retrievedUser);
    assertEquals(TEST_USERNAME, retrievedUser.username());
    assertEquals(TEST_EMAIL, retrievedUser.email());
  }

  @Test
  public void createUser_duplicate_fails() {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    assertThrows(DataAccessException.class, () -> {
      dataAccess.createUser(user);
      dataAccess.createUser(user); // Should throw exception
    });
  }

  @Test
  public void getUser_success() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    dataAccess.createUser(user);

    UserData retrievedUser = dataAccess.getUser(TEST_USERNAME);
    assertNotNull(retrievedUser);
    assertEquals(TEST_USERNAME, retrievedUser.username());
  }

  @Test
  public void getUser_nonexistent() throws DataAccessException {
    UserData retrievedUser = dataAccess.getUser("nonexistentUser");
    assertNull(retrievedUser);
  }

  // Game Tests
  @Test
  public void createGame_success() throws DataAccessException, BadRequestException {
    GameData game = new GameData(0, null, null, TEST_GAME_NAME, new ChessGame());
    dataAccess.createGame(game);

    Collection<GameData> games = dataAccess.listGames();
    assertFalse(games.isEmpty());
    assertEquals(1, games.size());
    GameData retrievedGame = games.iterator().next();
    assertEquals(TEST_GAME_NAME, retrievedGame.gameName());
  }

  @Test
  public void createGame_withInvalidUser_fails() {
    GameData game = new GameData(0, "nonexistentUser", null, TEST_GAME_NAME, new ChessGame());
    assertThrows(DataAccessException.class, () -> {
      dataAccess.createGame(game);
    });
  }

  @Test
  public void getGame_success() throws DataAccessException, BadRequestException {
    GameData game = new GameData(0, null, null, TEST_GAME_NAME, new ChessGame());
    dataAccess.createGame(game);
    Collection<GameData> games = dataAccess.listGames();
    int gameId = games.iterator().next().gameID();

    GameData retrievedGame = dataAccess.getGame(gameId);
    assertNotNull(retrievedGame);
    assertEquals(TEST_GAME_NAME, retrievedGame.gameName());
  }

  @Test
  public void getGame_nonexistent() {
    assertThrows(BadRequestException.class, () -> {
      dataAccess.getGame(999);
    });
  }

  @Test
  public void listGames_success() throws DataAccessException {
    GameData game1 = new GameData(0, null, null, "game1", new ChessGame());
    GameData game2 = new GameData(0, null, null, "game2", new ChessGame());

    dataAccess.createGame(game1);
    dataAccess.createGame(game2);

    Collection<GameData> games = dataAccess.listGames();
    assertEquals(2, games.size());
  }

  @Test
  public void listGames_emptyDatabase() throws DataAccessException {
    Collection<GameData> games = dataAccess.listGames();
    assertTrue(games.isEmpty());
  }

  @Test
  public void updateGame_success() throws DataAccessException, BadRequestException {
    GameData game = new GameData(0, null, null, TEST_GAME_NAME, new ChessGame());
    dataAccess.createGame(game);
    Collection<GameData> games = dataAccess.listGames();
    GameData createdGame = games.iterator().next();

    GameData updatedGame = new GameData(
            createdGame.gameID(),
            TEST_USERNAME,
            null,
            TEST_GAME_NAME,
            createdGame.game()
    );
    dataAccess.updateGame(updatedGame);

    GameData retrievedGame = dataAccess.getGame(createdGame.gameID());
    assertEquals(TEST_USERNAME, retrievedGame.whiteUsername());
  }

  @Test
  public void updateGame_nonexistent() {
    GameData nonexistentGame = new GameData(999, null, null, TEST_GAME_NAME, new ChessGame());
    assertThrows(DataAccessException.class, () -> {
      dataAccess.updateGame(nonexistentGame);
    });
  }

  // Auth Tests
  @Test
  public void createAuth_success() throws DataAccessException {
    // First create a user
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    dataAccess.createUser(user);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    dataAccess.createAuth(auth);

    AuthData retrievedAuth = dataAccess.getAuth(TEST_AUTH_TOKEN);
    assertNotNull(retrievedAuth);
    assertEquals(TEST_USERNAME, retrievedAuth.username());
  }

  @Test
  public void createAuth_invalidUser_fails() {
    AuthData auth = new AuthData("nonexistentUser", TEST_AUTH_TOKEN);
    assertThrows(DataAccessException.class, () -> {
      dataAccess.createAuth(auth);
    });
  }

  @Test
  public void getAuth_success() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    dataAccess.createUser(user);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    dataAccess.createAuth(auth);

    AuthData retrievedAuth = dataAccess.getAuth(TEST_AUTH_TOKEN);
    assertNotNull(retrievedAuth);
    assertEquals(TEST_AUTH_TOKEN, retrievedAuth.authToken());
  }

  @Test
  public void getAuth_nonexistent() throws DataAccessException {
    AuthData retrievedAuth = dataAccess.getAuth("nonexistentToken");
    assertNull(retrievedAuth);
  }

  @Test
  public void deleteAuth_success() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    dataAccess.createUser(user);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    dataAccess.createAuth(auth);

    dataAccess.deleteAuth(TEST_AUTH_TOKEN);
    AuthData retrievedAuth = dataAccess.getAuth(TEST_AUTH_TOKEN);
    assertNull(retrievedAuth);
  }

  @Test
  public void deleteAuth_nonexistent_noError() throws DataAccessException {
    // Should not throw an exception
    assertDoesNotThrow(() -> dataAccess.deleteAuth("nonexistentToken"));
  }

  @Test
  public void clear_success() throws DataAccessException {
    // Create some data first
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    dataAccess.createUser(user);

    GameData game = new GameData(0, null, null, TEST_GAME_NAME, new ChessGame());
    dataAccess.createGame(game);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    dataAccess.createAuth(auth);

    // Clear everything
    dataAccess.clear();

    // Verify everything is cleared
    assertNull(dataAccess.getUser(TEST_USERNAME));
    assertTrue(dataAccess.listGames().isEmpty());
    assertNull(dataAccess.getAuth(TEST_AUTH_TOKEN));
  }
}