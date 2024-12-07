package client;

import com.google.gson.Gson;
import model.GameData;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class HTTPDecoder {
  private final String baseURL;
  private final ServerFacade facade;
  private final Gson gson;

  public HTTPDecoder(ServerFacade facade, String serverDomain) {
    this.baseURL = "http://" + serverDomain;
    this.facade = facade;
    this.gson = new Gson();
  }

  public boolean register(String username, String password, String email) {
    var body = Map.of("username", username, "password", password, "email", email);
    Map resp = request("POST", "/user", gson.toJson(body));
    if (resp.containsKey("Error")) {
      return false;
    }
    facade.setAuthToken((String) resp.get("authToken"));
    return true;
  }

  public boolean login(String username, String password) {
    var body = Map.of("username", username, "password", password);
    Map resp = request("POST", "/session", gson.toJson(body));
    if (resp.containsKey("Error")) {
      return false;
    }
    facade.setAuthToken((String) resp.get("authToken"));
    return true;
  }

  public boolean logout() {
    Map resp = request("DELETE", "/session");
    if (resp.containsKey("Error")) {
      return false;
    }
    facade.setAuthToken(null);
    return true;
  }

  public int createGame(String gameName) {
    var body = Map.of("gameName", gameName);
    Map resp = request("POST", "/game", gson.toJson(body));
    System.out.println("gameName: " + gameName + resp + body);

    if (resp.containsKey("Error")) {
      return -1;
    }
    return ((Double) resp.get("gameID")).intValue();
  }

  public HashSet<GameData> listGames() {
    Map resp = request("GET", "/game");
    if (resp.containsKey("Error")) {
      return new HashSet<>();
    }

    var games = new HashSet<GameData>();
    var gamesList = (java.util.ArrayList<?>) resp.get("games");

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

  public boolean joinGame(int gameId, String playerColor) {
    Map<String, Object> body = new HashMap<>();
    body.put("gameID", gameId);
    if (playerColor != null) {
      body.put("playerColor", playerColor);
    }

    try {
      Map resp = request("PUT", "/game", gson.toJson(body));
      return !resp.containsKey("Error");
    } catch (Exception e) {
      return false;
    }
  }

  private Map request(String method, String endpoint) {
    return request(method, endpoint, null);
  }

  private Map request(String method, String endpoint, String body) {
    try {
      HttpURLConnection http = makeConnection(method, endpoint, body);

      try {
        if (http.getResponseCode() == 401) {
          return Map.of("Error", 401);
        }
      } catch (IOException e) {
        return Map.of("Error", 401);
      }

      try (InputStream respBody = http.getInputStream()) {
        return gson.fromJson(new InputStreamReader(respBody), Map.class);
      }
    } catch (URISyntaxException | IOException e) {
      return Map.of("Error", e.getMessage());
    }
  }

  private HttpURLConnection makeConnection(String method, String endpoint, String body)
          throws URISyntaxException, IOException {
    URI uri = new URI(baseURL + endpoint);
    HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
    http.setRequestMethod(method);

    if (facade.getAuthToken() != null) {
      http.addRequestProperty("authorization", facade.getAuthToken());
    }

    if (!Objects.equals(body, null)) {
      http.setDoOutput(true);
      http.addRequestProperty("Content-Type", "application/json");
      try (var outputStream = http.getOutputStream()) {
        outputStream.write(body.getBytes());
      }
    }

    http.connect();
    return http;
  }
}
