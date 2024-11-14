package client;

import com.google.gson.Gson;
import model.GameData;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;

public class ServerFacade {
  String baseURL;
  String authToken;

  public ServerFacade() {
    this.baseURL = "http://localhost:8080";
  }

  public ServerFacade(int port) {
    this.baseURL = "http://localhost:" + port;
  }

  public boolean register(String username, String password, String email) {
    try {
      URI uri = new URI(baseURL + "/user");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.addRequestProperty("Content-Type", "application/json");

      var requestBody = Map.of("username", username, "password", password, "email", email);
      try (OutputStream os = http.getOutputStream()) {
        String jsonBody = new Gson().toJson(requestBody);
        os.write(jsonBody.getBytes());
      }

      http.connect();

      if (http.getResponseCode() == 200) {
        try (InputStream respBody = http.getInputStream()) {
          InputStreamReader reader = new InputStreamReader(respBody);
          Map<String, String> map = new Gson().fromJson(reader, Map.class);
          authToken = map.get("authToken");
          return true;
        }
      }
    } catch (Exception ex) {
      return false;
    }
    return false;
  }

  public boolean login(String username, String password) {
    try {
      URI uri = new URI(baseURL + "/session");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.addRequestProperty("Content-Type", "application/json");

      var requestBody = Map.of("username", username, "password", password);
      try (OutputStream os = http.getOutputStream()) {
        String jsonBody = new Gson().toJson(requestBody);
        os.write(jsonBody.getBytes());
      }

      http.connect();

      if (http.getResponseCode() == 200) {
        try (InputStream respBody = http.getInputStream()) {
          InputStreamReader reader = new InputStreamReader(respBody);
          Map<String, String> map = new Gson().fromJson(reader, Map.class);
          authToken = map.get("authToken");
          return true;
        }
      }
    } catch (Exception ex) {
      return false;
    }
    return false;
  }

  public boolean logout() {
    try {
      URI uri = new URI(baseURL + "/session");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("DELETE");
      if (authToken != null) {
        http.addRequestProperty("Authorization", authToken);
      }

      http.connect();

      if (http.getResponseCode() == 200) {
        authToken = null;
        return true;
      }
    } catch (Exception ex) {
      return false;
    }
    return false;
  }

  public int createGame(String gameName) {
    try {
      URI uri = new URI(baseURL + "/game");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      if (authToken != null) {
        http.addRequestProperty("Authorization", authToken);
      }
      http.addRequestProperty("Content-Type", "application/json");

      var requestBody = Map.of("gameName", gameName);
      try (OutputStream os = http.getOutputStream()) {
        String jsonBody = new Gson().toJson(requestBody);
        os.write(jsonBody.getBytes());
      }

      http.connect();

      if (http.getResponseCode() == 200) {
        try (InputStream respBody = http.getInputStream()) {
          InputStreamReader reader = new InputStreamReader(respBody);
          Map<String, Double> map = new Gson().fromJson(reader, Map.class);
          return map.get("gameID").intValue();
        }
      }
    } catch (Exception ex) {
      return -1;
    }
    return -1;
  }

  public HashSet<GameData> listGames() {
    try {
      URI uri = new URI(baseURL + "/game");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("GET");
      if (authToken != null) {
        http.addRequestProperty("Authorization", authToken);
      }

      http.connect();

      if (http.getResponseCode() == 200) {
        try (InputStream respBody = http.getInputStream()) {
          InputStreamReader reader = new InputStreamReader(respBody);
          var response = new Gson().fromJson(reader, Map.class);
          var games = new HashSet<GameData>();
          var gamesList = (java.util.ArrayList<?>) response.get("games");
          for (Object gameObj : gamesList) {
            var gameMap = (Map<?, ?>) gameObj;
            var game = new GameData(
                    ((Double) gameMap.get("gameID")).intValue(),
                    (String) gameMap.get("whiteUsername"),
                    (String) gameMap.get("blackUsername"),
                    (String) gameMap.get("gameName"),
                    new chess.ChessGame()
            );
            games.add(game);
          }
          return games;
        }
      }
    } catch (Exception ex) {
      return new HashSet<>();
    }
    return new HashSet<>();
  }

  public boolean joinGame(int gameID, String playerColor) {
    try {
      URI uri = new URI(baseURL + "/game");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("PUT");
      http.setDoOutput(true);
      if (authToken != null) {
        http.addRequestProperty("Authorization", authToken);
      }
      http.addRequestProperty("Content-Type", "application/json");

      var requestBody = Map.of("gameID", gameID, "playerColor", playerColor);
      try (OutputStream os = http.getOutputStream()) {
        String jsonBody = new Gson().toJson(requestBody);
        os.write(jsonBody.getBytes());
      }

      http.connect();
      return http.getResponseCode() == 200;

    } catch (Exception ex) {
      return false;
    }
  }
}