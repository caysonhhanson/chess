package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.UnauthorizedException;
import service.GameService;
import spark.Request;
import spark.Response;
import java.util.Map;

public class GameHandler {
  private final GameService gameService;
  private final Gson gson;

  public GameHandler(GameService gameService) {
    this.gameService = gameService;
    this.gson = new Gson();
  }

  public Object handleClear(Request req, Response res) {
    try {
      gameService.clear();
      res.status(200);
      return "{}";
    } catch (DataAccessException e) {
      res.status(500);
      return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
  }
  public Object handleListGames(Request req, Response res) {
    try {
      // Get auth token from header
      String authToken = req.headers("authorization");
      if (authToken == null) {
        res.status(401);
        return gson.toJson(Map.of("message", "Error: unauthorized"));
      }

      // Get games list
      var games = gameService.listGames(authToken);

      // Return success response
      res.status(200);
      return gson.toJson(Map.of("games", games));
    } catch (UnauthorizedException e) {
      res.status(401);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (DataAccessException e) {
      res.status(500);
      return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
  }
}

