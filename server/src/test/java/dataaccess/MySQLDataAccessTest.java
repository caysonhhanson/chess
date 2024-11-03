package dataaccess;

import chess.ChessGame;
import model.*;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLDataAccessTest {
  private SQLUserDAO userDAO;
  private SQLGameDAO gameDAO;
  private SQLAuthDAO authDAO;

  private final UserData defaultUser = new UserData("username", "password", "email");
  private final AuthData defaultAuth = new AuthData("username", "token");
  private GameData defaultGameData;

  @BeforeEach
  void setUp() throws DataAccessException {
    DatabaseInitializer.initialize();

    userDAO = new SQLUserDAO();
    gameDAO = new SQLGameDAO();
    authDAO = new SQLAuthDAO();

    authDAO.clear();
    gameDAO.clear();
    userDAO.clear();


    ChessGame defaultChessGame = new ChessGame();
    defaultGameData = new GameData(1234, "white", "black", "gamename", defaultChessGame);
  }

  @Test
  void createUserPositive() throws DataAccessException {
    userDAO.createUser(defaultUser);

    UserData retrievedUser = userDAO.getUser(defaultUser.username());
    assertNotNull(retrievedUser);
    assertEquals(defaultUser.username(), retrievedUser.username());
    assertEquals(defaultUser.email(), retrievedUser.email());
    assertTrue(BCrypt.checkpw(defaultUser.password(), retrievedUser.password()));
  }

  @Test
  void createUserDuplicateFails() throws DataAccessException {
    userDAO.createUser(defaultUser);
    DataAccessException exception = assertThrows(DataAccessException.class,
            () -> userDAO.createUser(defaultUser)
    );
    assertEquals("Error: already taken", exception.getMessage());
  }

  @Test
  void getUserPositive() throws DataAccessException {
    userDAO.createUser(defaultUser);
    UserData retrievedUser = userDAO.getUser(defaultUser.username());
    assertNotNull(retrievedUser);
    assertEquals(defaultUser.username(), retrievedUser.username());
    assertEquals(defaultUser.email(), retrievedUser.email());
    assertTrue(BCrypt.checkpw(defaultUser.password(), retrievedUser.password()));
  }

  @Test
  void getUserNonexistent() throws DataAccessException {
    assertNull(userDAO.getUser(defaultUser.username()));
  }

  @Test
  void createAuthPositive() throws DataAccessException {
    userDAO.createUser(defaultUser);

    authDAO.createAuth(defaultAuth);
    AuthData retrieved = authDAO.getAuth(defaultAuth.authToken());
    assertNotNull(retrieved);
    assertEquals(defaultAuth.username(), retrieved.username());
    assertEquals(defaultAuth.authToken(), retrieved.authToken());
  }

  @Test
  void getAuthPositive() throws DataAccessException {
    userDAO.createUser(defaultUser);

    authDAO.createAuth(defaultAuth);
    AuthData retrieved = authDAO.getAuth(defaultAuth.authToken());
    assertNotNull(retrieved);
    assertEquals(defaultAuth.username(), retrieved.username());
    assertEquals(defaultAuth.authToken(), retrieved.authToken());
  }

  @Test
  void getAuthNonexistent() throws DataAccessException {
    assertNull(authDAO.getAuth("nonexistentToken"));
  }

  @Test
  void deleteAuthPositive() throws DataAccessException {
    userDAO.createUser(defaultUser);

    authDAO.createAuth(defaultAuth);
    authDAO.deleteAuth(defaultAuth.authToken());
    assertNull(authDAO.getAuth(defaultAuth.authToken()));
  }

  @Test
  void deleteAuthNonexistent() throws DataAccessException {
    assertDoesNotThrow(() -> authDAO.deleteAuth("nonexistentToken"));
  }

  @Test
  void createGamePositive() throws DataAccessException {
    // First create users for white and black players
    userDAO.createUser(new UserData("white", "pass", "white@email.com"));
    userDAO.createUser(new UserData("black", "pass", "black@email.com"));

    gameDAO.createGame(defaultGameData);

    Collection<GameData> games = gameDAO.listGames();
    assertEquals(1, games.size());
    GameData retrieved = games.iterator().next();
    assertEquals(defaultGameData.whiteUsername(), retrieved.whiteUsername());
    assertEquals(defaultGameData.blackUsername(), retrieved.blackUsername());
    assertEquals(defaultGameData.gameName(), retrieved.gameName());
  }

  @Test
  void listGamesPositive() throws DataAccessException {
    userDAO.createUser(new UserData("white", "pass", "white@email.com"));
    userDAO.createUser(new UserData("black", "pass", "black@email.com"));

    gameDAO.createGame(defaultGameData);
    GameData secondGame = new GameData(2345, "white", "black", "game2", new ChessGame());
    gameDAO.createGame(secondGame);

    Collection<GameData> games = gameDAO.listGames();
    assertEquals(2, games.size());
  }

  @Test
  void listGamesEmpty() throws DataAccessException {
    Collection<GameData> games = gameDAO.listGames();
    assertTrue(games.isEmpty());
  }

  @Test
  void updateGamePositive() throws DataAccessException {
    userDAO.createUser(new UserData("white", "pass", "white@email.com"));
    userDAO.createUser(new UserData("black", "pass", "black@email.com"));
    userDAO.createUser(new UserData("newWhite", "pass", "new@email.com"));

    GameData initialGame = new GameData(0, "white", "black", "testGame", new ChessGame());
    gameDAO.createGame(initialGame);

    Collection<GameData> games = gameDAO.listGames();
    GameData createdGame = games.iterator().next();
    int actualGameId = createdGame.gameID();

    GameData updatedGame = new GameData(
            actualGameId,
            "newWhite",
            createdGame.blackUsername(),
            createdGame.gameName(),
            createdGame.game()
    );

    gameDAO.updateGame(updatedGame);

    GameData retrievedGame = gameDAO.getGame(actualGameId);
    assertNotNull(retrievedGame);
    assertEquals("newWhite", retrievedGame.whiteUsername());
    assertEquals("black", retrievedGame.blackUsername());
    assertEquals("testGame", retrievedGame.gameName());
  }

  @Test
  void updateGameNonexistent() {
    GameData nonexistentGame = new GameData(999, null, null, "gameName", new ChessGame());
    DataAccessException exception = assertThrows(DataAccessException.class,
            () -> gameDAO.updateGame(nonexistentGame)
    );
    assertEquals("Error: game not found", exception.getMessage());
  }

  @Test
  void clearUserPositive() throws DataAccessException {
    userDAO.createUser(defaultUser);
    userDAO.clear();
    assertNull(userDAO.getUser(defaultUser.username()));
  }

  @Test
  void clearAuthPositive() throws DataAccessException {
    userDAO.createUser(defaultUser);
    authDAO.createAuth(defaultAuth);
    authDAO.clear();
    assertNull(authDAO.getAuth(defaultAuth.authToken()));
  }

  @Test
  void clearGamePositive() throws DataAccessException {
    userDAO.createUser(new UserData("white", "pass", "white@email.com"));
    userDAO.createUser(new UserData("black", "pass", "black@email.com"));
    gameDAO.createGame(defaultGameData);
    gameDAO.clear();
    assertTrue(gameDAO.listGames().isEmpty());
  }
}