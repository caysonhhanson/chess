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
  public void clear_success() throws DataAccessException {
    // Create test data
    UserData user = new UserData("testUser", "password123", "test@example.com");
    dataAccess.createUser(user);
    GameData game = new GameData(0, "testUser", null, "Test Game", new ChessGame());
    dataAccess.createGame(game);
    AuthData auth = new AuthData("testUser", "testAuthToken");
    dataAccess.createAuth(auth);

    // Clear all data
    assertDoesNotThrow(() -> dataAccess.clear());

    // Verify everything is cleared
    assertNull(dataAccess.getUser("testUser"));
    assertTrue(dataAccess.listGames().isEmpty());
    assertNull(dataAccess.getAuth("testAuthToken"));
  }
}