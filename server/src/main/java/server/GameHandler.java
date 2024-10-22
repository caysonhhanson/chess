package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
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
}
