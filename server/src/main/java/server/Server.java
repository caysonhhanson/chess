
package server;

import dataaccess.*;
import service.UserService;
import service.GameService;
import spark.Spark;

public class Server {
    private final UserService userService;
    private final GameService gameService;
    public static GameDAO gameDAO;  // Make static so handler can access
    public static AuthDAO authDAO;   // Make static so handler can access

    public Server() {
        try {
            DatabaseInitializer.initialize();
            var userDAO = new SQLUserDAO();
            authDAO = new SQLAuthDAO();    // Assign to static field
            gameDAO = new SQLGameDAO();    // Assign to static field
            userService = new UserService(userDAO, authDAO);
            gameService = new GameService(userDAO, gameDAO, authDAO);
        } catch (DataAccessException e) {
            System.err.println("Failed to initialize server: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        configureWebSocket();
        configureEndpoints();

        Spark.awaitInitialization();
        return Spark.port();
    }

    private void configureWebSocket() {
        System.out.println("🔧 [SERVER] Configuring WebSocket endpoint at /ws");
        try {
            Spark.webSocket("/ws", WebSocketHandler.class);

        } catch (Exception e) {
            System.err.println("❌ [SERVER] WebSocket configuration failed:");
            e.printStackTrace();
            throw new RuntimeException("Failed to configure WebSocket", e);
        }
    }

    private void configureEndpoints() {
        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            return "OK";
        });

        var userHandler = new UserHandler(userService);
        var gameHandler = new GameHandler(gameService);

        Spark.delete("/db", gameHandler::handleClear);
        Spark.post("/user", userHandler::handleRegister);
        Spark.post("/session", userHandler::handleLogin);
        Spark.delete("/session", userHandler::handleLogout);
        Spark.get("/game", gameHandler::handleListGames);
        Spark.post("/game", gameHandler::handleCreateGame);
        Spark.put("/game", gameHandler::handleJoinGame);
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}

