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
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        // Create handlers for user and game services
        var userHandler = new UserHandler(userService);
        var gameHandler = new GameHandler(gameService);

        // Set up the API routes
        Spark.delete("/db", gameHandler::handleClear);
        Spark.post("/user", userHandler::handleRegister);
        Spark.post("/session", userHandler::handleLogin);
        Spark.delete("/session", userHandler::handleLogout);
        Spark.get("/game", gameHandler::handleListGames);
        Spark.post("/game", gameHandler::handleCreateGame);
        Spark.put("/game", gameHandler::handleJoinGame);

        // Start the server
        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
    }
}


