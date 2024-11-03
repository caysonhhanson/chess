package server;
import dataaccess.*;
import service.UserService;
import service.GameService;
import spark.Spark;

public class Server {
    private final UserService userService;
    private final GameService gameService;
    public Server() {
        try {
            DatabaseInitializer.initialize();
            var userDAO = new SQLUserDAO();
            var authDAO = new SQLAuthDAO();
            var gameDAO = new SQLGameDAO();
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
        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
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
        Spark.awaitInitialization();
        return Spark.port();
    }
    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}

