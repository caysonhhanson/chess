package client;

import chess.ChessGame;
import com.google.gson.*;
import websocket.commands.*;
import websocket.messages.*;
import websocket.messages.Error;

import javax.websocket.*;
import java.net.URI;
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
  private final CountDownLatch messageLatch = new CountDownLatch(1);

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
    System.out.println("\n🔍 [WS-DEBUG] Starting connection process...");
    System.out.println("🔍 [WS-DEBUG] Server URL: " + serverUrl);

    WebSocketContainer container = ContainerProvider.getWebSocketContainer();

    // Set and log container properties
    int bufferSize = 65535;
    container.setDefaultMaxTextMessageBufferSize(bufferSize);
    container.setDefaultMaxSessionIdleTimeout(0); // No timeout
    System.out.println("🔍 [WS-DEBUG] Container configured:");
    System.out.println("  - Buffer size: " + bufferSize);
    System.out.println("  - Session timeout: 0 (disabled)");

    // Connect with retries and detailed logging
    int maxRetries = 3;
    int attempt = 0;
    Exception lastException = null;

    while (attempt < maxRetries) {
      attempt++;
      System.out.println("\n🔍 [WS-DEBUG] Connection attempt " + attempt + " of " + maxRetries);

      try {
        URI uri = new URI(serverUrl);
        System.out.println("🔍 [WS-DEBUG] Parsed URI: " + uri);
        System.out.println("  - Scheme: " + uri.getScheme());
        System.out.println("  - Host: " + uri.getHost());
        System.out.println("  - Port: " + uri.getPort());
        System.out.println("  - Path: " + uri.getPath());

        this.session = container.connectToServer(this, uri);
        System.out.println("🔍 [WS-DEBUG] Initial connection established");
        System.out.println("  - Session ID: " + (session != null ? session.getId() : "null"));
        System.out.println("  - Session state: " + (session != null ? (session.isOpen() ? "open" : "closed") : "null"));

        System.out.println("🔍 [WS-DEBUG] Waiting for onOpen confirmation...");
        if (connectLatch.await(5, TimeUnit.SECONDS)) {
          System.out.println("✅ [WS-DEBUG] Connection fully established and confirmed");
          System.out.println("  - Final session state: " + (session != null ? (session.isOpen() ? "open" : "closed") : "null"));
          return;
        } else {
          System.out.println("❌ [WS-DEBUG] Connection timeout waiting for onOpen");
          throw new Exception("Connection timeout - onOpen never called");
        }

      } catch (Exception e) {
        lastException = e;
        System.err.println("\n❌ [WS-DEBUG] Connection attempt " + attempt + " failed:");
        System.err.println("  - Error type: " + e.getClass().getSimpleName());
        System.err.println("  - Error message: " + e.getMessage());
        e.printStackTrace();

        if (attempt < maxRetries) {
          int waitTime = 1000 * attempt; // Exponential backoff
          System.out.println("⏳ [WS-DEBUG] Waiting " + waitTime + "ms before retry...");
          Thread.sleep(waitTime);
        }
      }
    }

    System.err.println("\n❌ [WS-DEBUG] All connection attempts failed");
    if (lastException != null) {
      System.err.println("  - Final error: " + lastException.getMessage());
      throw lastException;
    }
  }

  public void sendCommand(UserGameCommand command) {
    System.out.println("\n📤 [WS-CLIENT] Preparing to send command: " + command.getCommandType());
    if (session != null && session.isOpen()) {
      try {
        String jsonCommand = gson.toJson(command);
        System.out.println("📤 [WS-CLIENT] Sending command JSON: " + jsonCommand);

        // Use synchronous send for better reliability in test environment
        session.getBasicRemote().sendText(jsonCommand);
        System.out.println("✅ [WS-CLIENT] Command sent successfully");

        // Reset message latch for next message
        messageLatch.countDown();
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
    this.session = session;
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
      messageLatch.countDown();
    } catch (Exception e) {
      System.err.println("❌ [WS-CLIENT] Error processing message: " + e.getMessage());
      e.printStackTrace();
      errorHandler.accept("Error processing message: " + e.getMessage());
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
        e.printStackTrace();
      }
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    System.out.println("\n🔴 [WS-CLIENT] WebSocket connection closed");
    System.out.println("🔴 [WS-CLIENT] Close reason: " + reason);
  }

  @OnError
  public void onError(Throwable error) {
    System.err.println("\n❌ [WS-CLIENT] WebSocket error occurred: " + error.getMessage());
    error.printStackTrace();
    errorHandler.accept("WebSocket error: " + error.getMessage());
  }
}