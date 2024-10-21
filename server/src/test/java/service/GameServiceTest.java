package service;

import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
  private GameService service;
  private DataAccess dataAccess;

  @BeforeEach
  public void setUp() {
    dataAccess = new dataaccess.MemoryDataAccess();
    service = new GameService(dataAccess);
  }

  @Test
  public void clearSuccess() throws DataAccessException {
    service.register("testUser", "password", "email@test.com");
    service.clear();
    assertThrows(DataAccessException.class, () -> dataAccess.getUser("testUser"));
  }

  @Test
  public void registerSuccess() throws DataAccessException {
    AuthData result = service.register("testUser", "1234", "test@gmail.com");

    assertNotNull(result);
    assertNotNull(result.authToken());
    assertEquals("testUser", result.username());
  }

  @Test
  public void registerFailDuplicate() throws DataAccessException {
    service.register("testUser", "password", "email@test.com");

    assertThrows(DataAccessException.class, () ->
            service.register("testUser", "password", "email@test.com"));
  }

  @Test
  public void registerFailBadRequest() {
    assertThrows(DataAccessException.class, () ->
            service.register(null, "password", "email@test.com"));
    assertThrows(DataAccessException.class, () ->
            service.register("", "password", "email@test.com"));
  }
}
