package client;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class ServerFacade {
  private final String serverUrl;
  private final Gson gson;
  private String authToken;

  public ServerFacade(String serverUrl) {
    this.serverUrl = serverUrl;
    this.gson = new Gson();
  }
}