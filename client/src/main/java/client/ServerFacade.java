package client;

import com.google.gson.Gson;
import model.GameData;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;

public class ServerFacade {
  private final String baseURL;
  private String authToken;

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

      var requestBody = Map.of(
              "username", username,
              "password", password,
              "email", email
      );

      writeBody(http, requestBody);
      http.connect();

      if (http.getResponseCode() == 200) {
        authToken = readToken(http);
        return true;
      }
      return false;
    } catch (Exception ex) {
      return false;
    }
  }

  public boolean login(String username, String password) {
    try {
      URI uri = new URI(baseURL + "/session");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.addRequestProperty("Content-Type", "application/json");

      var requestBody = Map.of(
              "username", username,
              "password", password
      );

      writeBody(http, requestBody);
      http.connect();

      if (http.getResponseCode() == 200) {
        authToken = readToken(http);
        return true;
      }
      return false;
    } catch (Exception ex) {
      return false;
    }
  }

  public boolean logout() {
    if (authToken == null) return false;

    try {
      URI uri = new URI(baseURL + "/session");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("DELETE");
      http.addRequestProperty("Authorization", authToken);

      http.connect();

      if (http.getResponseCode() == 200) {
        authToken = null;
        return true;
      }
      return false;
    } catch (Exception ex) {
      return false;
    }
  }

  public int createGame(String gameName) {
    if (authToken == null) return -1;

    try {
      URI uri = new URI(baseURL + "/game");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.addRequestProperty("Authorization", authToken);
      http.addRequestProperty("Content-Type", "application/json");

      var requestBody = Map.of("gameName", gameName);
      writeBody(http, requestBody);

      http.connect();

      if (http.getResponseCode() == 200) {
        try (InputStream respBody = http.getInputStream()) {
          Map<String, Double> map = new Gson().fromJson(new InputStreamReader(respBody), Map.class);
          return map.get("gameID").intValue();
        }
      }
      return -1;
    } catch (Exception ex) {
      return -1;
    }
  }

  public HashSet<GameData> listGames() {
    if (authToken == null) return new HashSet<>();

    try {
      URI uri = new URI(baseURL + "/game");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("GET");
      http.addRequestProperty("Authorization", authToken);

      http.connect();

      if (http.getResponseCode() == 200) {
        try (InputStream respBody = http.getInputStream()) {
          var response = new Gson().fromJson(new InputStreamReader(respBody), Map.class);
          var games = new HashSet<GameData>();
          var gamesList = (java.util.ArrayList<?>) response.get("games");

          for (Object gameObj : gamesList) {
            Map<?, ?> gameMap = (Map<?, ?>) gameObj;
            games.add(new GameData(
                    ((Double) gameMap.get("gameID")).intValue(),
                    (String) gameMap.get("whiteUsername"),
                    (String) gameMap.get("blackUsername"),
                    (String) gameMap.get("gameName"),
                    new chess.ChessGame()
            ));
          }
          return games;
        }
      }
      return new HashSet<>();
    } catch (Exception ex) {
      return new HashSet<>();
    }
  }

  public boolean joinGame(int gameID, String playerColor) {
    if (authToken == null) return false;

    try {
      URI uri = new URI(baseURL + "/game");
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("PUT");
      http.setDoOutput(true);
      http.addRequestProperty("Authorization", authToken);
      http.addRequestProperty("Content-Type", "application/json");

      var requestBody = Map.of(
              "gameID", gameID,
              "playerColor", playerColor
      );

      writeBody(http, requestBody);
      http.connect();

      return http.getResponseCode() == 200;
    } catch (Exception ex) {
      return false;
    }
  }

  private void writeBody(HttpURLConnection http, Object request) throws IOException {
    if (request != null) {
      http.addRequestProperty("Content-Type", "application/json");
      String jsonData = new Gson().toJson(request);
      try (var outputStream = http.getOutputStream()) {
        outputStream.write(jsonData.getBytes());
      }
    }
  }

  private String readToken(HttpURLConnection http) throws IOException {
    try (InputStream respBody = http.getInputStream()) {
      return new Gson().fromJson(new InputStreamReader(respBody), Map.class).get("authToken").toString();
    }
  }
}