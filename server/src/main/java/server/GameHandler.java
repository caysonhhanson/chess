package server;

import com.google.gson.Gson;
import dataaccess.AlreadyTakenException;
import dataaccess.BadRequestException;
import dataaccess.DataAccessException;
import dataaccess.UnauthorizedException;
import service.*;
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
      String authToken = req.headers("authorization");
      if (authToken == null) {
        res.status(401);
        return gson.toJson(Map.of("message", "Error: unauthorized"));
      }

      var games = gameService.listGames(authToken);
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

  public Object handleCreateGame(Request req, Response res) {
    try {
      // Get auth token from header
      String authToken = req.headers("authorization");
      if (authToken == null) {
        res.status(401);
        return gson.toJson(Map.of("message", "Error: unauthorized"));
      }

      // Parse request body
      record CreateGameRequest(String gameName) {}
      var request = gson.fromJson(req.body(), CreateGameRequest.class);

      // Create the game
      var result = gameService.createGame(authToken, request.gameName());

      // Return success response
      res.status(200);
      return gson.toJson(result);
    } catch (UnauthorizedException e) {
      res.status(401);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (BadRequestException e) {
      res.status(400);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (DataAccessException e) {
      res.status(500);
      return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
  }
  public Object handleJoinGame(Request req, Response res) {
    res.type("application/json");

    try {
      // Get authToken from header
      String authToken = req.headers("authorization");

      // Parse request body
      record JoinGameRequest(String playerColor, Integer gameID) { }
      var joinRequest = gson.fromJson(req.body(), JoinGameRequest.class);

      gameService.joinGame(authToken, joinRequest.playerColor(), joinRequest.gameID());
      res.status(200);
      return "{}";
    } catch (UnauthorizedException e) {
      res.status(401);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (BadRequestException e) {
      res.status(400);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (AlreadyTakenException e) {
      res.status(403);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (Exception e) {
      res.status(500);
      return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
  }
}

