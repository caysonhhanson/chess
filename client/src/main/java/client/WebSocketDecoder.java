package client;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.*;
import websocket.commands.*;
import websocket.messages.*;
import websocket.messages.Error;

import javax.websocket.*;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@ClientEndpoint
public class WebSocketDecoder {
  private Session session;
  private final String serverUrl;
  private final Consumer<String> notificationHandler;
  private final Consumer<ChessGame> gameUpdateHandler;
  private final Consumer<String> errorHandler;
  private final Gson gson;
  private final CountDownLatch connectLatch = new CountDownLatch(1);

  public WebSocketDecoder(String serverUrl, Consumer<String> notificationHandler,
                          Consumer<ChessGame> gameUpdateHandler, Consumer<String> errorHandler) {
    System.out.println("🔧 [WS-CLIENT] Initializing WebSocketDecoder with URL: " + serverUrl);
    this.serverUrl = serverUrl;
    this.notificationHandler = notificationHandler;
    this.gameUpdateHandler = gameUpdateHandler;
    this.errorHandler = errorHandler;

    // Configure Gson with type adapters
    this.gson = new GsonBuilder()
            .registerTypeAdapter(ServerMessage.class, (JsonDeserializer<ServerMessage>) (json, typeOfT, context) -> {
              System.out.println("🔄 [WS-CLIENT] Deserializing message: " + json);
              JsonObject jsonObject = json.getAsJsonObject();
              String type = jsonObject.get("serverMessageType").getAsString();
              System.out.println("🔄 [WS-CLIENT] Message type: " + type);

              return switch (type) {
                case "NOTIFICATION" -> context.deserialize(json, Notification.class);
                case "ERROR" -> context.deserialize(json, Error.class);
                case "LOAD_GAME" -> context.deserialize(json, LoadGame.class);
                default -> throw new JsonParseException("Unknown message type: " + type);
              };
            }).create();
  }

  public void connect() throws Exception {
    System.out.println("🔌 [WS-CLIENT] Attempting to connect to: " + serverUrl);
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();

    // Set container properties
    container.setDefaultMaxTextMessageBufferSize(65535);
    container.setDefaultMaxSessionIdleTimeout(10000);

    // Connect with retries
    int maxRetries = 3;
    int attempt = 0;
    Exception lastException = null;

    while (attempt < maxRetries) {
      try {
        this.session = container.connectToServer(this, new URI(serverUrl));
        System.out.println("🔌 [WS-CLIENT] Connection established");

        // Wait for connection to complete
        if (connectLatch.await(5, TimeUnit.SECONDS)) {
          System.out.println("✅ [WS-CLIENT] Connection confirmed");
          return;
        } else {
          System.out.println("⚠️ [WS-CLIENT] Connect latch timeout");
          throw new Exception("Connection timeout");
        }
      } catch (Exception e) {
        lastException = e;
        attempt++;
        System.out.println("⚠️ [WS-CLIENT] Connection attempt " + attempt + " failed: " + e.getMessage());
        if (attempt < maxRetries) {
          Thread.sleep(1000); // Wait before retry
        }
      }
    }

    if (lastException != null) {
      throw lastException;
    }
  }

  public void disconnect() {
    System.out.println("🔌 [WS-CLIENT] Disconnecting...");
    if (session != null && session.isOpen()) {
      try {
        session.close();
        System.out.println("✅ [WS-CLIENT] Disconnected successfully");
      } catch (Exception e) {
        System.err.println("❌ [WS-CLIENT] Error during disconnect: " + e.getMessage());
        errorHandler.accept("Error closing connection: " + e.getMessage());
      }
    } else {
      System.out.println("ℹ️ [WS-CLIENT] No active session to disconnect");
    }
  }

  public void sendCommand(UserGameCommand command) {
    System.out.println("\n📤 [WS-CLIENT] Preparing to send command: " + command.getCommandType());
    if (session != null && session.isOpen()) {
      try {
        String jsonCommand = gson.toJson(command);
        System.out.println("📤 [WS-CLIENT] Sending command JSON: " + jsonCommand);

        // Use async send with completion callback
        CompletableFuture<Void> future = new CompletableFuture<>();
        session.getAsyncRemote()
                .sendText(jsonCommand, result -> {
                  if (result.isOK()) {
                    System.out.println("✅ [WS-CLIENT] Command sent successfully");
                    future.complete(null);
                  } else {
                    String error = "Failed to send command: " + result.getException();
                    System.err.println("❌ [WS-CLIENT] " + error);
                    future.completeExceptionally(result.getException());
                  }
                });

        // Wait for send completion
        future.get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        System.err.println("❌ [WS-CLIENT] Error sending command: " + e.getMessage());
        e.printStackTrace();
        errorHandler.accept("Error sending command: " + e.getMessage());
      }
    } else {
      String error = "Cannot send command - not connected to server";
      System.err.println("❌ [WS-CLIENT] " + error);
      errorHandler.accept(error);
    }
  }

  @OnOpen
  public void onOpen(Session session) {
    System.out.println("🔌 [WS-CLIENT] WebSocket connection opened");
    System.out.println("ℹ️ [WS-CLIENT] Session ID: " + session.getId());
    connectLatch.countDown();
  }

  @OnMessage
  public void onMessage(String message) {
    System.out.println("\n📥 [WS-CLIENT] Received message: " + message);
    try {
      ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
      System.out.println("🔄 [WS-CLIENT] Parsed message type: " + serverMessage.getServerMessageType());

      switch (serverMessage.getServerMessageType()) {
        case NOTIFICATION -> {
          String notification = ((Notification)serverMessage).getMessage();
          System.out.println("📢 [WS-CLIENT] Processing notification: " + notification);
          notificationHandler.accept(notification);
        }
        case ERROR -> {
          String error = ((Error)serverMessage).getErrorMessage();
          System.out.println("❌ [WS-CLIENT] Processing error: " + error);
          errorHandler.accept(error);
        }
        case LOAD_GAME -> {
          ChessGame game = ((LoadGame)serverMessage).getGame();
          System.out.println("🎮 [WS-CLIENT] Processing game update");
          gameUpdateHandler.accept(game);
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [WS-CLIENT] Error processing message: ");
      e.printStackTrace();
      errorHandler.accept("Error processing message: " + e.getMessage());
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    System.out.println("\n🔌 [WS-CLIENT] WebSocket connection closed");
    System.out.println("ℹ️ [WS-CLIENT] Close reason: " + reason);
  }

  @OnError
  public void onError(Throwable error) {
    System.err.println("\n❌ [WS-CLIENT] WebSocket error occurred: ");
    error.printStackTrace();
    errorHandler.accept("WebSocket error: " + error.getMessage());
  }
}