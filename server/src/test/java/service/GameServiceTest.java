package service;

import chess.ChessGame;
import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
  private GameService gameService;
  private DataAccess dataAccess;
  private UserService userService; // Needed for setup

  @BeforeEach
  public void setUp() {
    dataAccess=new MemoryDataAccess();
    gameService=new GameService(dataAccess);
    userService=new UserService(dataAccess);
  }

  @Test
  public void clearSuccess() throws DataAccessException {
    // Register a user first
    userService.register("testUser", "password", "email@test.com");

    // Clear the database
    gameService.clear();

    // Verify user no longer exists
    UserData user=dataAccess.getUser("testUser");
    assertNull(user);
  }

  @Test
  public void testListGames_Success() throws DataAccessException, UnauthorizedException {
    // Create a test auth token
    String authToken = "test-auth-token";
    String username = "testUser";
    dataAccess.createAuth(new AuthData(username, authToken));

    // Create some test games
    var game1 = new GameData(0, "white1", "black1", "game1", new ChessGame());
    var game2 = new GameData(0, "white2", "black2", "game2", new ChessGame());
    dataAccess.createGame(game1);
    dataAccess.createGame(game2);

    // Get the games list
    var games = gameService.listGames(authToken);

    // Verify results
    assertNotNull(games);
    assertEquals(2, games.size());
  }

  @Test
  public void testListGames_Unauthorized() {
    // Test with invalid auth token
    String invalidAuthToken = "invalid-token";

    // Verify the service throws an exception
    assertThrows(UnauthorizedException.class, () -> {
      gameService.listGames(invalidAuthToken);
    });
  }
}