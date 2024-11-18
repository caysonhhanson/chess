package client;

import model.GameData;
import java.util.HashSet;

public class ServerFacade {
  private final HTTPDecoder http;
  private String authToken;
  private final String serverDomain;

  public ServerFacade(int port) {
    this.serverDomain = "localhost:" + port;
    this.http = new HTTPDecoder(this, serverDomain);
  }

  protected String getAuthToken() {
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
}