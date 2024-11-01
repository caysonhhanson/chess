package server;

import dataaccess.MemoryDataAccess;
import service.UserService;
import service.GameService;
import spark.Spark;

public class Server {
    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        var dataAccess = new MemoryDataAccess();
        var userService = new UserService(dataAccess);
        var gameService = new GameService(dataAccess);
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
    }

    public void stop() {
        Spark.stop();
    }
}