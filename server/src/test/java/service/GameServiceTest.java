package service;

import chess.ChessGame;
import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
  private GameService gameService;
  private UserService userService;
  private UserDAO userDAO;
  private GameDAO gameDAO;
  private AuthDAO authDAO;

  @BeforeEach
  public void setUp() {
    userDAO = new MemoryUserDAO(); // Assuming you have this DAO implemented
    authDAO = new MemoryAuthDAO(); // Assuming you have this DAO implemented
    gameDAO = new MemoryGameDAO(); // Assuming you have this DAO implemented
    userService = new UserService(userDAO, authDAO);
    gameService = new GameService(userDAO, gameDAO, authDAO);
  }

  @Test
  public void clearSuccess() throws DataAccessException {
    // Register a user first
    userService.register("testUser", "password", "email@test.com");

    // Clear the database
    gameService.clear();

    // Verify user no longer exists
    UserData user = userDAO.getUser("testUser"); // Use userDAO for user retrieval
    assertNull(user);
  }

  @Test
  public void testListGamesSuccess() throws DataAccessException, UnauthorizedException {
    // Create a test auth token
    String authToken = "test-auth-token";
    String username = "testUser";
    authDAO.createAuth(new AuthData(username, authToken)); // Use authDAO for auth creation

    // Create some test games
    var game1 = new GameData(0, "white1", "black1", "game1", new ChessGame());
    var game2 = new GameData(0, "white2", "black2", "game2", new ChessGame());
    gameDAO.createGame(game1); // Use gameDAO for game creation
    gameDAO.createGame(game2);

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
    authDAO.createAuth(new AuthData(username, authToken)); // Use authDAO for auth creation

    // Create a game
    String gameName = "Test Game";
    var result = gameService.createGame(authToken, gameName);

    // Verify the game was created
    assertNotNull(result);
    assertTrue(result.gameID() > 0);

    // Verify the game exists in the database
    GameData game = gameDAO.getGame(result.gameID()); // Use gameDAO for game retrieval
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
