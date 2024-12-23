package client;

import model.GameData;
import java.util.HashSet;

public class ServerFacade {
  private final HTTPDecoder http;
  private String authToken;
  private final String serverDomain;
  private final int port;

  public ServerFacade(int port) {
    this.port = port;
    this.serverDomain = "localhost";
    this.http = new HTTPDecoder(this, getServerUrl());
  }

  public String getAuthToken() {
    return authToken;
  }

  protected void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  public boolean register(String username, String password, String email) {
    return http.register(username, password, email);
  }

  public boolean login(String username, String password) {
    return http.login(username, password);
  }

  public boolean logout() {
    return http.logout();
  }

  public int createGame(String gameName) {
    return http.createGame(gameName);
  }

  public HashSet<GameData> listGames() {
    return http.listGames();
  }

  public boolean joinGame(int gameId, String playerColor) {
    return http.joinGame(gameId, playerColor);
  }

  public String getServerUrl() {
    return serverDomain + ":" + port;
  }

  public String getWebSocketUrl() {
    return "ws://" + getServerUrl() + "/ws";
  }
}
