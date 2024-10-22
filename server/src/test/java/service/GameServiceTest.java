package service;

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
}