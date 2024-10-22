package server;

import dataaccess.MemoryDataAccess;
import service.UserService;
import spark.Spark;

public class Server {
    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        // Create services and handlers
        var dataAccess = new MemoryDataAccess();
        var userService = new UserService(dataAccess);
        var userHandler = new UserHandler(userService);

        // Register endpoints
        Spark.post("/user", userHandler::handleRegister);
        Spark.post("/session", userHandler::handleLogin);
        Spark.delete("/session", userHandler::handleLogout);

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
    }
}
