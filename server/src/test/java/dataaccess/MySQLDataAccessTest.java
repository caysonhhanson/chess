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
    // Initialize the database tables before each test
    DatabaseInitializer.initialize();

    userDAO = new SQLUserDAO();
    gameDAO = new SQLGameDAO();
    authDAO = new SQLAuthDAO();

    // Clear all data before each test
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
    // Don't check the password directly since it's hashed
    assertTrue(retrievedUser.password().startsWith("$2a")); // BCrypt hash prefix
  }

  @Test
  public void createUser_duplicate_fails() throws DataAccessException {
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    DataAccessException exception = assertThrows(DataAccessException.class, () -> {
      userDAO.createUser(user);
    });
    assertEquals("Error: already taken", exception.getMessage());
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
  public void createGame_success() throws DataAccessException {
    // First create a user since we have foreign key constraints
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    GameData game = new GameData(0, TEST_USERNAME, null, TEST_GAME_NAME, new ChessGame());
    gameDAO.createGame(game);

    Collection<GameData> games = gameDAO.listGames();
    assertFalse(games.isEmpty());
    assertEquals(1, games.size());
    GameData retrievedGame = games.iterator().next();
    assertEquals(TEST_GAME_NAME, retrievedGame.gameName());
    assertEquals(TEST_USERNAME, retrievedGame.whiteUsername());
  }

  @Test
  public void getGame_success() throws DataAccessException {
    // First create a user
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    // Create a game
    GameData game = new GameData(0, TEST_USERNAME, null, TEST_GAME_NAME, new ChessGame());
    gameDAO.createGame(game);

    // Get the game ID from the list
    Collection<GameData> games = gameDAO.listGames();
    int gameId = games.iterator().next().gameID();

    // Test getting the specific game
    GameData retrievedGame = gameDAO.getGame(gameId);
    assertNotNull(retrievedGame);
    assertEquals(TEST_GAME_NAME, retrievedGame.gameName());
    assertEquals(TEST_USERNAME, retrievedGame.whiteUsername());
  }

  @Test
  public void updateGame_success() throws DataAccessException {
    // First create a user
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);
    UserData user2 = new UserData("player2", TEST_PASSWORD, "p2@example.com");
    userDAO.createUser(user2);

    // Create initial game
    GameData game = new GameData(0, TEST_USERNAME, null, TEST_GAME_NAME, new ChessGame());
    gameDAO.createGame(game);

    // Get the created game's ID
    Collection<GameData> games = gameDAO.listGames();
    GameData createdGame = games.iterator().next();

    // Update the game
    GameData updatedGame = new GameData(
            createdGame.gameID(),
            createdGame.whiteUsername(),
            "player2",  // Add black player
            createdGame.gameName(),
            createdGame.game()
    );
    gameDAO.updateGame(updatedGame);

    // Verify update
    GameData retrievedGame = gameDAO.getGame(createdGame.gameID());
    assertEquals("player2", retrievedGame.blackUsername());
  }

  @Test
  public void updateGame_nonexistent() throws DataAccessException {
    GameData nonexistentGame = new GameData(999, null, null, TEST_GAME_NAME, new ChessGame());
    DataAccessException exception = assertThrows(DataAccessException.class, () -> {
      gameDAO.updateGame(nonexistentGame);
    });
    assertEquals("Error: game not found", exception.getMessage());
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
    assertEquals(TEST_AUTH_TOKEN, retrievedAuth.authToken());
  }

  @Test
  public void getAuth_success() throws DataAccessException {
    // First create a user
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    authDAO.createAuth(auth);

    AuthData retrievedAuth = authDAO.getAuth(TEST_AUTH_TOKEN);
    assertNotNull(retrievedAuth);
    assertEquals(TEST_USERNAME, retrievedAuth.username());
    assertEquals(TEST_AUTH_TOKEN, retrievedAuth.authToken());
  }

  @Test
  public void getAuth_nonexistent() throws DataAccessException {
    AuthData retrievedAuth = authDAO.getAuth("nonexistentToken");
    assertNull(retrievedAuth);
  }

  @Test
  public void deleteAuth_success() throws DataAccessException {
    // First create a user
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    // Create auth token
    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    authDAO.createAuth(auth);

    // Delete and verify
    authDAO.deleteAuth(TEST_AUTH_TOKEN);
    AuthData retrievedAuth = authDAO.getAuth(TEST_AUTH_TOKEN);
    assertNull(retrievedAuth);
  }

  @Test
  public void clear_success() throws DataAccessException {
    // Create test data
    UserData user = new UserData(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
    userDAO.createUser(user);

    GameData game = new GameData(0, TEST_USERNAME, null, TEST_GAME_NAME, new ChessGame());
    gameDAO.createGame(game);

    AuthData auth = new AuthData(TEST_USERNAME, TEST_AUTH_TOKEN);
    authDAO.createAuth(auth);

    // Clear all data
    userDAO.clear();
    gameDAO.clear();
    authDAO.clear();

    // Verify everything is cleared
    assertNull(userDAO.getUser(TEST_USERNAME));
    assertTrue(gameDAO.listGames().isEmpty());
    assertNull(authDAO.getAuth(TEST_AUTH_TOKEN));
  }
}