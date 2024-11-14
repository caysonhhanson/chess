package client;

import org.junit.jupiter.api.*;
import server.Server;
import java.util.Collection;
import model.GameData;
import java.net.URI;
import java.net.HttpURLConnection;
import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {
    private static Server server;
    private static ServerFacade facade;
    private static int port;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void setup() throws Exception {
        URI uri = new URI("http://localhost:" + port + "/db");
        HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
        http.setRequestMethod("DELETE");
        http.connect();
        try (var ignored = http.getInputStream()) {
        }
    }

    @Test
    public void registerPositive() {
        assertTrue(facade.register("testuser", "testpass", "test@email.com"));
    }

    @Test
    public void registerNegative() {
        assertTrue(facade.register("testuser", "testpass", "test@email.com"));
        assertFalse(facade.register("testuser", "testpass", "test@email.com"));
    }

    @Test
    public void loginPositive() {
        assertTrue(facade.register("testuser", "testpass", "test@email.com"));
        assertTrue(facade.login("testuser", "testpass"));
    }

    @Test
    public void loginNegative() {
        assertFalse(facade.login("wronguser", "wrongpass"));
    }

    @Test
    public void logoutPositive() {
        assertTrue(facade.register("testuser", "testpass", "test@email.com"));
        assertTrue(facade.logout());
    }

    @Test
    public void logoutNegative() {
        assertFalse(facade.logout());
    }

    @Test
    public void createGamePositive() {
        assertTrue(facade.register("testuser", "testpass", "test@email.com"));
        assertTrue(facade.createGame("testGame") >= 0);
    }

    @Test
    public void createGameNegative() {
        assertEquals(-1, facade.createGame("testGame"));
    }

    @Test
    public void listGamesPositive() {
        assertTrue(facade.register("testuser", "testpass", "test@email.com"));
        assertTrue(facade.createGame("game1") >= 0);
        Collection<GameData> games = facade.listGames();
        assertEquals(1, games.size());
    }

    @Test
    public void listGamesNegative() {
        assertTrue(facade.listGames().isEmpty());
    }

    @Test
    public void joinGamePositive() {
        assertTrue(facade.register("testuser", "testpass", "test@email.com"));
        int gameId = facade.createGame("testGame");
        assertTrue(facade.joinGame(gameId, "WHITE"));
    }

    @Test
    public void joinGameNegative() {
        assertTrue(facade.register("testuser", "testpass", "test@email.com"));
        int gameId = facade.createGame("testGame");
        assertTrue(facade.joinGame(gameId, "WHITE"));
        assertFalse(facade.joinGame(gameId, "WHITE"));
    }
}