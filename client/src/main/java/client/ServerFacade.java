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

}