package client;

import chess.ChessGame;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import model.GameData;

public class ServerFacade {
  String baseURL = "http://localhost:8080";
  String authToken;

  ServerFacade() {
  }
  public boolean register(String username, String password, String email) {
    var body = Map.of("username", username, "password", password, "email", email);
    var jsonBody = new Gson().toJson(body);
    return post("/user", jsonBody);
  }

  public boolean post(String endpoint, String body) {
    try {
      URI uri = new URI(baseURL + endpoint);
      HttpURLConnection http = (HttpURLConnection) uri.toURL().openConnection();
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      http.addRequestProperty("Content-Type", "application/json");
      try (var outputStream = http.getOutputStream()) {
        outputStream.write(body.getBytes());
      }
      http.connect();
      try {
        if (http.getResponseCode() == 401) {
          return false;
        }
      } catch (IOException e) {
        return false;
      }
      try (InputStream respBody = http.getInputStream()) {
        InputStreamReader inputStreamReader = new InputStreamReader(respBody);
        authToken = (String) new Gson().fromJson(inputStreamReader, Map.class).get("authToken");
      }
    } catch (URISyntaxException | IOException e) {
      return false;
    }
    return true;
  }
}