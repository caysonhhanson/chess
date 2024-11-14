package server;

import dataaccess.*;
import service.UserService;
import service.GameService;
import spark.Spark;

public class Server {
    private final UserService userService;
    private final GameService gameService;

    public Server() {
        var userDAO = new SQLUserDAO();
        var authDAO = new SQLAuthDAO();
        var gameDAO = new SQLGameDAO();

        userService = new UserService(userDAO, authDAO);
        gameService = new GameService(userDAO, gameDAO, authDAO);
    }

    public int run(int desiredPort) {
        try {
            // Set up dynamic database name for tests
            if (desiredPort == 0) {
                DatabaseManager.setDatabaseName("chessDb" + System.currentTimeMillis());
            }

            // Initialize database
            DatabaseInitializer.initialize();

            Spark.stop();
            Spark.awaitStop();

            Spark.port(desiredPort);
            Spark.staticFiles.location("web");

            var userHandler = new UserHandler(userService);
            var gameHandler = new GameHandler(gameService);

            Spark.delete("/db", gameHandler::handleClear);
            Spark.post("/user", userHandler::handleRegister);
            Spark.post("/session", userHandler::handleLogin);
            Spark.delete("/session", userHandler::handleLogout);
            Spark.get("/game", gameHandler::handleListGames);
            Spark.post("/game", gameHandler::handleCreateGame);
            Spark.put("/game", gameHandler::handleJoinGame);

            Spark.awaitInitialization();
            return Spark.port();
        } catch (DataAccessException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            return -1;
        }
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}

