package dataaccess;

import chess.ChessGame;
import model.*;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLDataAccessTest {
  private SQLUserDAO userDAO;
  private SQLGameDAO gameDAO;
  private SQLAuthDAO authDAO;
  private static final String TEST_USERNAME = "testUser";
  private static final String TEST_PASSWORD = "testPass";
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_AUTH_TOKEN = "testAuthToken";
  private static final String TEST_GAME_NAME = "testGame";

  @BeforeEach
  public void setUp() throws DataAccessException {
    userDAO = new SQLUserDAO();
    gameDAO = new SQLGameDAO();
    authDAO = new SQLAuthDAO();

    userDAO.clear();
    gameDAO.clear();
    authDAO.clear();
  }

  // User Tests
  @Test
  public void createUser_success() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    UserData retrievedUser = userDAO.getUser(TEST_USERNAME);
    assertNotNull(retrievedUser);
    assertEquals(TEST_USERNAME, retrievedUser.username());
    assertEquals(TEST_EMAIL, retrievedUser.email());
  }

  @Test
  public void createUser_duplicate_fails() {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    assertThrows(DataAccessException.class, () -> {
      userDAO.createUser(user);
      userDAO.createUser(user); // Should throw exception
    });
  }

  @Test
  public void getUser_success() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    UserData retrievedUser = userDAO.getUser(TEST_USERNAME);
    assertNotNull(retrievedUser);
    assertEquals(TEST_USERNAME, retrievedUser.username());
  }

  @Test
  public void getUser_nonexistent() throws DataAccessException {
    UserData retrievedUser = userDAO.getUser("nonexistentUser");
    assertNull(retrievedUser);
  }

  // Game Tests
  @Test
  public void createGameSuccess() throws DataAccessException, BadRequestException {
    GameData game = new GameData(0, TEST_USERNAME, null, TEST_GAME_NAME, new ChessGame());
    gameDAO.createGame(game);

    Collection<GameData> games = gameDAO.listGames();
    assertFalse(games.isEmpty());
    assertEquals(1, games.size());
    GameData retrievedGame = games.iterator().next();
    assertEquals(TEST_GAME_NAME, retrievedGame.gameName());
  }

  @Test
  public void createGame_withInvalidUser_fails() {
    GameData game = new GameData(0, "nonexistentUser", null, TEST_GAME_NAME, new ChessGame());
    assertThrows(DataAccessException.class, () -> {
      gameDAO.createGame(game);
    });
  }

  @Test
  public void getGame_success() throws DataAccessException, BadRequestException {
    GameData game = new GameData(0, TEST_USERNAME, null, TEST_GAME_NAME, new ChessGame());
    gameDAO.createGame(game);
    Collection<GameData> games = gameDAO.listGames();
    int gameId = games.iterator().next().gameID();

    GameData retrievedGame = gameDAO.getGame(gameId);
    assertNotNull(retrievedGame);
    assertEquals(TEST_GAME_NAME, retrievedGame.gameName());
  }

  @Test
  public void getGame_nonexistent() {
    assertThrows(BadRequestException.class, () -> {
      gameDAO.getGame(999);
    });
  }

  @Test
  public void listGames_success() throws DataAccessException {
    GameData game1 = new GameData(0, TEST_USERNAME, null, "game1", new ChessGame());
    GameData game2 = new GameData(0, TEST_USERNAME, null, "game2", new ChessGame());

    gameDAO.createGame(game1);
    gameDAO.createGame(game2);

    Collection<GameData> games = gameDAO.listGames();
    assertEquals(2, games.size());
  }

  @Test
  public void listGames_emptyDatabase() throws DataAccessException {
    Collection<GameData> games = gameDAO.listGames();
    assertTrue(games.isEmpty());
  }

  @Test
  public void updateGame_success() throws DataAccessException, BadRequestException {
    GameData game = new GameData(0, TEST_USERNAME, "tim", TEST_GAME_NAME, new ChessGame());
    gameDAO.createGame(game);
    Collection<GameData> games = gameDAO.listGames();
    GameData createdGame = games.iterator().next();

    GameData updatedGame = new GameData(createdGame.gameID(), "wilson", "tim", TEST_GAME_NAME, createdGame.game());
    gameDAO.updateGame(updatedGame);

    GameData retrievedGame = gameDAO.getGame(createdGame.gameID());
    assertEquals("wilson", retrievedGame.whiteUsername());
  }

  @Test
  public void updateGame_nonexistent() {
    GameData nonexistentGame = new GameData(999, null, null, TEST_GAME_NAME, new ChessGame());
    assertThrows(DataAccessException.class, () -> {
      gameDAO.updateGame(nonexistentGame);
    });
  }

  // Auth Tests
  @Test
  public void createAuth_success() throws DataAccessException {
    // First create a user
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    authDAO.createAuth(auth);

    AuthData retrievedAuth = authDAO.getAuth(TEST_AUTH_TOKEN);
    assertNotNull(retrievedAuth);
    assertEquals(TEST_USERNAME, retrievedAuth.username());
  }

  @Test
  public void createAuth_invalidUser_fails() {
    AuthData auth = new AuthData("nonexistentUser", TEST_AUTH_TOKEN);
    assertThrows(DataAccessException.class, () -> {
      authDAO.createAuth(auth);
    });
  }

  @Test
  public void getAuth_success() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    authDAO.createAuth(auth);

    AuthData retrievedAuth = authDAO.getAuth(TEST_AUTH_TOKEN);
    assertNotNull(retrievedAuth);
    assertEquals(TEST_AUTH_TOKEN, retrievedAuth.authToken());
  }

  @Test
  public void getAuth_nonexistent() throws DataAccessException {
    AuthData retrievedAuth = authDAO.getAuth("nonexistentToken");
    assertNull(retrievedAuth);
  }

  @Test
  public void deleteAuth_success() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    authDAO.createAuth(auth);

    authDAO.deleteAuth(TEST_AUTH_TOKEN);
    AuthData retrievedAuth = authDAO.getAuth(TEST_AUTH_TOKEN);
    assertNull(retrievedAuth);
  }

  @Test
  public void deleteAuth_nonexistent_noError() throws DataAccessException {
    // Should not throw an exception
    assertDoesNotThrow(() -> authDAO.deleteAuth("nonexistentToken"));
  }

  @Test
  public void clear_success() throws DataAccessException {
    // Create some data first
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    GameData game = new GameData(0, TEST_USERNAME, null, TEST_GAME_NAME, new ChessGame());
    gameDAO.createGame(game);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    authDAO.createAuth(auth);

    // Clear everything
    userDAO.clear();
    gameDAO.clear();
    authDAO.clear();

    // Verify everything is cleared
    assertNull(userDAO.getUser(TEST_USERNAME));
    assertTrue(gameDAO.listGames().isEmpty());
    assertNull(authDAO.getAuth(TEST_AUTH_TOKEN));
  }
}
