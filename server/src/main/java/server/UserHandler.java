package server;

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
      // Parse request body
      RegisterRequest registerRequest = gson.fromJson(req.body(), RegisterRequest.class);

      // Call service to register user
      AuthData auth = userService.register(
              registerRequest.username(),
              registerRequest.password(),
              registerRequest.email()
      );

      // Create and return success response
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
      // Parse request body
      LoginRequest loginRequest = gson.fromJson(req.body(), LoginRequest.class);

      // Call service to login user
      AuthData auth = userService.login(
              loginRequest.username(),
              loginRequest.password()
      );

      // Create and return success response
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
      // Get auth token from header
      String authToken = req.headers("authorization");

      // Call service to logout
      userService.logout(new AuthData(authToken, null));

      // Return success response
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