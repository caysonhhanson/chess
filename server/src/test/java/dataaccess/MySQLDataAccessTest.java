package dataaccess;

import chess.ChessGame;
import model.*;
import org.junit.jupiter.api.*;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class MySQLDataAccessTest {
  private static MySQLDataAccess dataAccess;

  @BeforeAll
  public static void init() {
    dataAccess = new MySQLDataAccess();
  }

  @BeforeEach
  public void setup() throws DataAccessException {
    dataAccess.clear();
  }
  @Test
  public void clearSuccess() throws DataAccessException {
    UserData user = new UserData("testUser", "password123", "test@example.com");
    dataAccess.createUser(user);
    GameData game = new GameData(0, "testUser", null, "Test Game", new ChessGame());
    dataAccess.createGame(game);
    AuthData auth = new AuthData("testUser", "testAuthToken");
    dataAccess.createAuth(auth);

    assertDoesNotThrow(() -> dataAccess.clear());


    assertNull(dataAccess.getUser("testUser"));
    assertTrue(dataAccess.listGames().isEmpty());
    assertNull(dataAccess.getAuth("testAuthToken"));
  }
  @Test
  public void createUserSuccess() throws DataAccessException {
    UserData user = new UserData("testUser", "password123", "test@example.com");
    assertDoesNotThrow(() -> dataAccess.createUser(user));

    UserData retrievedUser = dataAccess.getUser("testUser");
    assertNotNull(retrievedUser);
    assertEquals("testUser", retrievedUser.username());
    assertEquals("test@example.com", retrievedUser.email());
  }

  @Test
  public void createUserDuplicate() throws DataAccessException {
    UserData user = new UserData("testUser", "password123", "test@example.com");
    dataAccess.createUser(user);

    assertThrows(DataAccessException.class, () -> dataAccess.createUser(user));
  }

  @Test
  public void createGameSuccess() throws DataAccessException {
    // Create a user first due to foreign key constraints
    UserData user = new UserData("testUser", "password123", "test@example.com");
    dataAccess.createUser(user);

    GameData game = new GameData(0, "testUser", null, "Test Game", new ChessGame());
    assertDoesNotThrow(() -> dataAccess.createGame(game));

    Collection<GameData> games = dataAccess.listGames();
    assertEquals(1, games.size());
    GameData retrievedGame = games.iterator().next();
    assertEquals("Test Game", retrievedGame.gameName());
    assertEquals("testUser", retrievedGame.whiteUsername());
  }

}