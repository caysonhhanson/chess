package service;

import chess.ChessGame;
import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
  private GameService gameService;
  private DataAccess dataAccess;
  private UserService userService;

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
  public void testListGamesSuccess() throws DataAccessException, UnauthorizedException {
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
  public void testListGamesUnauthorized() {
    // Test with invalid auth token
    String invalidAuthToken = "invalid-token";

    // Verify the service throws an exception
    assertThrows(UnauthorizedException.class, () -> {
      gameService.listGames(invalidAuthToken);
    });
  }
  @Test
  public void testCreateGameSuccess() throws DataAccessException, UnauthorizedException, BadRequestException {
    // Create a test auth token
    String authToken = "test-auth-token";
    String username = "testUser";
    dataAccess.createAuth(new AuthData(username, authToken));

    // Create a game
    String gameName = "Test Game";
    var result = gameService.createGame(authToken, gameName);

    // Verify the game was created
    assertNotNull(result);
    assertTrue(result.gameID() > 0);

    // Verify the game exists in the database
    GameData game = dataAccess.getGame(result.gameID());
    assertNotNull(game);
    assertEquals(gameName, game.gameName());
    assertNull(game.whiteUsername());
    assertNull(game.blackUsername());
    assertNotNull(game.game());
  }

  @Test
  public void testCreateGameUnauthorized() {
    String invalidAuthToken = "invalid-token";
    String gameName = "Test Game";

    assertThrows(UnauthorizedException.class, () -> {
      gameService.createGame(invalidAuthToken, gameName);
    });
  }

  @Test
  public void testCreateGameBadRequest() {
    String authToken = "test-auth-token";

    assertThrows(BadRequestException.class, () -> {
      gameService.createGame(authToken, null);
    });

    assertThrows(BadRequestException.class, () -> {
      gameService.createGame(authToken, "");
    });

    assertThrows(BadRequestException.class, () -> {
      gameService.createGame(authToken, "   ");
    });
  }
}