package client;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.*;
import websocket.commands.*;
import websocket.messages.*;
import websocket.messages.Error;

import javax.websocket.*;
import java.net.URI;
import java.util.function.Consumer;

@ClientEndpoint
public class WebSocketDecoder {
  private Session session;
  private final String serverUrl;
  private final Consumer<String> notificationHandler;
  private final Consumer<ChessGame> gameUpdateHandler;
  private final Consumer<String> errorHandler;
  private Gson gson;

  public WebSocketDecoder(String serverUrl, Consumer<String> notificationHandler,
                          Consumer<ChessGame> gameUpdateHandler, Consumer<String> errorHandler) {
    this.serverUrl = serverUrl;
    this.notificationHandler = notificationHandler;
    this.gameUpdateHandler = gameUpdateHandler;
    this.errorHandler = errorHandler;
    this.gson = new GsonBuilder().create();

    JsonDeserializer<ServerMessage> deserializer = (json, typeOfT, context) -> {
      JsonObject jsonObject = json.getAsJsonObject();
      String type = jsonObject.get("serverMessageType").getAsString();

      return switch (type) {
        case "NOTIFICATION" -> gson.fromJson(json, Notification.class);
        case "ERROR" -> gson.fromJson(json, Error.class);
        case "LOAD_GAME" -> gson.fromJson(json, LoadGame.class);
        default -> throw new JsonParseException("Unknown message type: " + type);
      };
    };

    this.gson = new GsonBuilder()
            .registerTypeAdapter(ServerMessage.class, deserializer)
            .create();
  }

  public void connect() throws Exception {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    session = container.connectToServer(this, new URI(serverUrl));
  }

  public void disconnect() {
    if (session != null && session.isOpen()) {
      try {
        session.close();
      } catch (Exception e) {
        System.err.println("Error closing WebSocket: " + e.getMessage());
      }
    }
  }

  public void sendCommand(UserGameCommand command) {
    if (session != null && session.isOpen()) {
      String jsonCommand = gson.toJson(command);
      session.getAsyncRemote().sendText(jsonCommand);
    }
  }

  @OnMessage
  public void onMessage(String message) {
    try {
      ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

      switch (serverMessage.getServerMessageType()) {
        case NOTIFICATION -> notificationHandler.accept(((Notification)serverMessage).getMessage());
        case ERROR -> errorHandler.accept(((Error)serverMessage).getErrorMessage());
        case LOAD_GAME -> gameUpdateHandler.accept(((LoadGame)serverMessage).getGame());
      }
    } catch (Exception e) {
      errorHandler.accept("Error processing message: " + e.getMessage());
    }
  }

  @OnError
  public void onError(Throwable error) {
    errorHandler.accept("WebSocket error: " + error.getMessage());
  }
}