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
    initializeDAOs();
    clearDatabase();
    setupDefaultGame();
  }

  // Helper Methods
  private void initializeDAOs() {
    userDAO = new SQLUserDAO();
    gameDAO = new SQLGameDAO();
    authDAO = new SQLAuthDAO();
  }

  private void clearDatabase() throws DataAccessException {
    authDAO.clear();
    gameDAO.clear();
    userDAO.clear();
  }

  private void setupDefaultGame() {
    ChessGame defaultChessGame = new ChessGame();
    defaultGameData = new GameData(1234, "white", "black", "gamename", defaultChessGame);
  }

  private UserData createAndVerifyUser(String username, String password, String email) throws DataAccessException {
    UserData user = new UserData(username, password, email);
    userDAO.createUser(user);
    return userDAO.getUser(username);
  }

  private void setupUsersForGame() throws DataAccessException {
    createAndVerifyUser("white", "pass", "white@email.com");
    createAndVerifyUser("black", "pass", "black@email.com");
    createAndVerifyUser("newWhite", "pass", "new@email.com");
  }

  private GameData createAndGetGame(GameData game) throws DataAccessException {
    gameDAO.createGame(game);
    Collection<GameData> games = gameDAO.listGames();
    return games.iterator().next();
  }

  private void createDefaultUser() throws DataAccessException {
    userDAO.createUser(defaultUser);
  }

  private void createDefaultAuth() throws DataAccessException {
    createDefaultUser();
    authDAO.createAuth(defaultAuth);
  }

  private void verifyAuthData(AuthData expected, AuthData actual) {
    assertNotNull(actual);
    assertEquals(expected.username(), actual.username());
    assertEquals(expected.authToken(), actual.authToken());
  }

  private void verifyUserData(UserData expected, UserData actual) {
    assertNotNull(actual);
    assertEquals(expected.username(), actual.username());
    assertEquals(expected.email(), actual.email());
    assertTrue(BCrypt.checkpw(expected.password(), actual.password()));
  }

  // User Tests
  @Test
  void createUserPositive() throws DataAccessException {
    createDefaultUser();
    UserData retrievedUser = userDAO.getUser(defaultUser.username());
    verifyUserData(defaultUser, retrievedUser);
  }

  @Test
  void createUserDuplicateFails() throws DataAccessException {
    createDefaultUser();
    DataAccessException exception = assertThrows(DataAccessException.class,
            () -> userDAO.createUser(defaultUser)
    );
    assertEquals("Error: already taken", exception.getMessage());
  }

  @Test
  void getUserPositive() throws DataAccessException {
    createDefaultUser();
    UserData retrievedUser = userDAO.getUser(defaultUser.username());
    verifyUserData(defaultUser, retrievedUser);
  }

  @Test
  void getUserNonexistent() throws DataAccessException {
    assertNull(userDAO.getUser(defaultUser.username()));
  }

  // Auth Tests
  @Test
  void createAuthPositive() throws DataAccessException {
    createDefaultUser();
    authDAO.createAuth(defaultAuth);
    AuthData retrieved = authDAO.getAuth(defaultAuth.authToken());
    verifyAuthData(defaultAuth, retrieved);
  }

  @Test
  void createAuthNegative() throws DataAccessException {
    AuthData invalidAuth = new AuthData("nonexistentUser", "token123");
    assertThrows(DataAccessException.class, () -> authDAO.createAuth(invalidAuth));
  }

  @Test
  void getAuthPositive() throws DataAccessException {
    createDefaultAuth();
    AuthData retrieved = authDAO.getAuth(defaultAuth.authToken());
    verifyAuthData(defaultAuth, retrieved);
  }

  @Test
  void getAuthNonexistent() throws DataAccessException {
    assertNull(authDAO.getAuth("nonexistentToken"));
  }

  @Test
  void deleteAuthPositive() throws DataAccessException {
    createDefaultAuth();
    authDAO.deleteAuth(defaultAuth.authToken());
    assertNull(authDAO.getAuth(defaultAuth.authToken()));
  }

  @Test
  void deleteAuthNonexistent() throws DataAccessException {
    assertDoesNotThrow(() -> authDAO.deleteAuth("nonexistentToken"));
  }

  // Game Tests
  @Test
  void createGamePositive() throws DataAccessException {
    setupUsersForGame();
    GameData createdGame = createAndGetGame(defaultGameData);

    Collection<GameData> games = gameDAO.listGames();
    assertEquals(1, games.size());
    assertEquals(defaultGameData.whiteUsername(), createdGame.whiteUsername());
    assertEquals(defaultGameData.blackUsername(), createdGame.blackUsername());
    assertEquals(defaultGameData.gameName(), createdGame.gameName());
  }

  @Test
  void createGameNegative() throws DataAccessException {
    GameData invalidGame = new GameData(0, "nonexistentUser", null, "testGame", new ChessGame());
    assertThrows(DataAccessException.class, () -> gameDAO.createGame(invalidGame));
  }

  @Test
  void getGamePositive() throws DataAccessException {
    setupUsersForGame();
    GameData createdGame = createAndGetGame(defaultGameData);

    GameData retrievedGame = gameDAO.getGame(createdGame.gameID());
    assertNotNull(retrievedGame);
    assertEquals(createdGame.gameID(), retrievedGame.gameID());
    assertEquals(createdGame.whiteUsername(), retrievedGame.whiteUsername());
    assertEquals(createdGame.blackUsername(), retrievedGame.blackUsername());
  }

  @Test
  void getGameNegative() throws DataAccessException {
    assertNull(gameDAO.getGame(-1));
  }

  @Test
  void listGamesPositive() throws DataAccessException {
    setupUsersForGame();
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
    setupUsersForGame();
    GameData initialGame = new GameData(0, "white", "black", "testGame", new ChessGame());
    GameData createdGame = createAndGetGame(initialGame);

    GameData updatedGame = new GameData(
            createdGame.gameID(),
            "newWhite",
            createdGame.blackUsername(),
            createdGame.gameName(),
            createdGame.game()
    );

    gameDAO.updateGame(updatedGame);
    GameData retrievedGame = gameDAO.getGame(createdGame.gameID());
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

  // Clear Tests
  @Test
  void clearUserPositive() throws DataAccessException {
    createDefaultUser();
    userDAO.clear();
    assertNull(userDAO.getUser(defaultUser.username()));
  }

  @Test
  void clearUserNegative() throws DataAccessException {
    userDAO.clear();
    assertNull(userDAO.getUser("nonexistent"));
  }

  @Test
  void clearAuthPositive() throws DataAccessException {
    createDefaultAuth();
    authDAO.clear();
    assertNull(authDAO.getAuth(defaultAuth.authToken()));
  }

  @Test
  void clearAuthNegative() throws DataAccessException {
    authDAO.clear();
    assertNull(authDAO.getAuth("nonexistent"));
  }

  @Test
  void clearGamePositive() throws DataAccessException {
    setupUsersForGame();
    gameDAO.createGame(defaultGameData);
    gameDAO.clear();
    assertTrue(gameDAO.listGames().isEmpty());
  }

  @Test
  void clearGameNegative() throws DataAccessException {
    gameDAO.clear();
    assertTrue(gameDAO.listGames().isEmpty());
  }
}