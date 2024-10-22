package server;

import dataaccess.*;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import service.UserService;
import spark.Request;
import spark.Response;
import java.util.Map;

public class UserHandler {
  private final UserService userService;
  private final Gson gson;

  public UserHandler(UserService userService) {
    this.userService = userService;
    this.gson = new Gson();
  }

  record RegisterRequest(String username, String password, String email) {}
  record LoginRequest(String username, String password) {}
  record AuthResponse(String username, String authToken) {}

  public Object handleRegister(Request req, Response res) {
    try {
      RegisterRequest registerRequest = gson.fromJson(req.body(), RegisterRequest.class);

      AuthData auth = userService.register(
              registerRequest.username(),
              registerRequest.password(),
              registerRequest.email()
      );

      res.status(200);
      return gson.toJson(new AuthResponse(auth.username(), auth.authToken()));
    } catch (DataAccessException e) {
      res.status(e.getMessage().contains("bad request") ? 400 : 403);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (Exception e) {
      res.status(500);
      return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
  }

  public Object handleLogin(Request req, Response res) {
    try {
      LoginRequest loginRequest = gson.fromJson(req.body(), LoginRequest.class);

      AuthData auth = userService.login(
              loginRequest.username(),
              loginRequest.password()
      );

      res.status(200);
      return gson.toJson(new AuthResponse(auth.username(), auth.authToken()));
    } catch (DataAccessException e) {
      res.status(401);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (Exception e) {
      res.status(500);
      return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
  }

  public Object handleLogout(Request req, Response res) {
    try {
      String authToken = req.headers("authorization");

      userService.logout(new AuthData(null, authToken));

      res.status(200);
      return "{}";
    } catch (DataAccessException e) {
      res.status(401);
      return gson.toJson(Map.of("message", e.getMessage()));
    } catch (Exception e) {
      res.status(500);
      return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
  }
}