package server;

import chess.*;
import com.google.gson.*;
import dataaccess.*;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.*;
import websocket.commands.*;
import websocket.messages.*;
import websocket.messages.Error;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler extends WebSocketAdapter {
  private static final Map<Integer, Map<Session, String>> gameConnections = new ConcurrentHashMap<>();
  private final GameDAO gameDAO;
  private final AuthDAO authDAO;
  private final Gson gson;
  private static final Map<Session, String> pendingMessages = new ConcurrentHashMap<>();

  public WebSocketHandler(GameDAO gameDAO, AuthDAO authDAO) {
    System.out.println("🔵 [WEBSOCKET-INIT] Creating new WebSocketHandler instance");
    this.gameDAO = gameDAO;
    this.authDAO = authDAO;

    this.gson = new GsonBuilder()
            .registerTypeAdapter(UserGameCommand.class, (JsonDeserializer<UserGameCommand>) (json, typeOfT, context) -> {
              JsonObject obj = json.getAsJsonObject();
              System.out.println("🔄 [WEBSOCKET-PARSE] Raw JSON: " + obj);
              String commandType = obj.get("commandType").getAsString();
              System.out.println("🔄 [WEBSOCKET-PARSE] Command type: " + commandType);

              return switch (commandType) {
                case "MAKE_MOVE" -> {
                  System.out.println("🔄 [WEBSOCKET-PARSE] Parsing MakeMove command");
                  yield context.deserialize(json, MakeMove.class);
                }
                case "CONNECT" -> {
                  if (obj.has("playerColor")) {
                    System.out.println("🔄 [WEBSOCKET-PARSE] Parsing JoinPlayer command");
                    yield context.deserialize(json, JoinPlayer.class);
                  } else {
                    System.out.println("🔄 [WEBSOCKET-PARSE] Parsing JoinObserver command");
                    yield context.deserialize(json, JoinObserver.class);
                  }
                }
                case "LEAVE" -> {
                  System.out.println("🔄 [WEBSOCKET-PARSE] Parsing Leave command");
                  yield context.deserialize(json, Leave.class);
                }
                case "RESIGN" -> {
                  System.out.println("🔄 [WEBSOCKET-PARSE] Parsing Resign command");
                  yield context.deserialize(json, Resign.class);
                }
                default -> throw new JsonParseException("Unknown command type: " + commandType);
              };
            })
            .create();
    System.out.println("🔵 [WEBSOCKET-INIT] Handler initialized");
  }

  @OnWebSocketConnect
  @Override
  public void onWebSocketConnect(Session session) {
    System.out.println("\n🟢 [WEBSOCKET-CONNECT] New connection from " + session.getRemoteAddress());
    super.onWebSocketConnect(session);
    session.setIdleTimeout(300000); // 5 minutes
    System.out.println("🟢 [WEBSOCKET-CONNECT] Session initialized with ID: " + session.hashCode());
  }

  @OnWebSocketMessage
  public void onWebSocketMessage(Session session, String message) {
    System.out.println("\n📥 [WEBSOCKET-MESSAGE] Received from session " + session.hashCode() + ": " + message);

    try {
      UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
      System.out.println("📦 [WEBSOCKET-MESSAGE] Parsed command type: " + command.getCommandType());

      // Store the message in case we need to retry
      pendingMessages.put(session, message);

      AuthData auth = authDAO.getAuth(command.getAuthToken());
      System.out.println("🔑 [WEBSOCKET-AUTH] Auth token check: " + (auth != null ? "valid" : "invalid"));

      if (auth == null) {
        System.out.println("❌ [WEBSOCKET-AUTH] Invalid auth token");
        sendError(session, "Error: unauthorized");
        return;
      }

      GameData game = gameDAO.getGame(command.getGameID());
      System.out.println("🎮 [WEBSOCKET-GAME] Game lookup: " + (game != null ? "found" : "not found"));

      if (game == null) {
        System.out.println("❌ [WEBSOCKET-GAME] Game not found");
        sendError(session, "Error: game not found");
        return;
      }

      processCommand(session, command, auth, game);

      // Clear the pending message after successful processing
      pendingMessages.remove(session);

    } catch (Exception e) {
      System.err.println("❌ [WEBSOCKET-ERROR] Error processing message:");
      e.printStackTrace();
      sendError(session, "Error: " + e.getMessage());
    }
  }

  private void processCommand(Session session, UserGameCommand command, AuthData auth, GameData game) {
    try {
      System.out.println("🔄 [WEBSOCKET-PROCESS] Processing command: " + command.getCommandType());

      switch (command.getCommandType()) {
        case CONNECT -> {
          System.out.println("🔄 [WEBSOCKET-PROCESS] Handling connect");
          handleConnect(session, command, auth, game);
        }
        case MAKE_MOVE -> {
          System.out.println("🔄 [WEBSOCKET-PROCESS] Handling move");
          handleMove(session, command, auth, game);
        }
        case RESIGN -> {
          System.out.println("🔄 [WEBSOCKET-PROCESS] Handling resign");
          handleResign(session, command, auth, game);
        }
        case LEAVE -> {
          System.out.println("🔄 [WEBSOCKET-PROCESS] Handling leave");
          handleLeave(session, command);
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [WEBSOCKET-PROCESS] Error processing command: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void handleConnect(Session session, UserGameCommand command, AuthData auth, GameData game) {
    try {
      System.out.println("\n🔄 [WEBSOCKET-CONNECT] Processing connect for user: " + auth.username());

      // Add to connections
      Map<Session, String> gameSessionMap = gameConnections.computeIfAbsent(command.getGameID(), k -> new ConcurrentHashMap<>());
      gameSessionMap.put(session, auth.username());
      System.out.println("✅ [WEBSOCKET-CONNECT] Added to game connections. Total connections: " + gameSessionMap.size());

      // Send game state
      LoadGame loadGameMessage = new LoadGame(game.game());
      String loadGameJson = gson.toJson(loadGameMessage);
      System.out.println("📤 [WEBSOCKET-CONNECT] Sending game state: " + loadGameJson);
      session.getRemote().sendString(loadGameJson);

      // Send join notification
      String notificationMessage;
      if (command instanceof JoinPlayer joinPlayer) {
        String color = joinPlayer.getPlayerColor();
        System.out.println("👤 [WEBSOCKET-CONNECT] Joining as " + color + " player");
        notificationMessage = String.format("%s joined as %s player", auth.username(), color);
      } else {
        System.out.println("👀 [WEBSOCKET-CONNECT] Joining as observer");
        notificationMessage = String.format("%s joined as an observer", auth.username());
      }

      System.out.println("📢 [WEBSOCKET-CONNECT] Broadcasting notification: " + notificationMessage);
      broadcastNotification(command.getGameID(), notificationMessage, session);

    } catch (Exception e) {
      System.err.println("❌ [WEBSOCKET-CONNECT] Error in connect handler: " + e.getMessage());
      e.printStackTrace();
      sendError(session, "Error during connect: " + e.getMessage());
    }
  }

  // ... [rest of the handlers remain the same but with added logging]

  private void cleanupSession(Session session) {
    gameConnections.values().forEach(sessions -> {
      String username = sessions.remove(session);
      if (username != null) {
        System.out.println("🧹 [WEBSOCKET-CLEANUP] Removed user: " + username);
        broadcast(sessions, gson.toJson(new Notification(username + " disconnected")));
      }
    });
  }

  private void handleMove(Session session, UserGameCommand command, AuthData auth, GameData game) throws Exception {
    System.out.println("🔄 [WEBSOCKET-MOVE] Processing move for user: " + auth.username());

    if (!(command instanceof MakeMove moveCommand)) {
      System.out.println("❌ [WEBSOCKET-MOVE] Invalid move command type");
      sendError(session, "Error: invalid move command");
      return;
    }

    ChessMove move = moveCommand.getMove();
    ChessGame chessGame = game.game();

    // Validate player and turn
    boolean isWhite = game.whiteUsername() != null && game.whiteUsername().equals(auth.username());
    boolean isBlack = game.blackUsername() != null && game.blackUsername().equals(auth.username());

    if (!isWhite && !isBlack) {
      System.out.println("❌ [WEBSOCKET-MOVE] Not a player in the game");
      sendError(session, "Error: not a player in the game");
      return;
    }

    if ((chessGame.getTeamTurn() == ChessGame.TeamColor.WHITE && !isWhite) ||
            (chessGame.getTeamTurn() == ChessGame.TeamColor.BLACK && !isBlack)) {
      System.out.println("❌ [WEBSOCKET-MOVE] Not player's turn");
      sendError(session, "Error: not your turn");
      return;
    }

    try {
      // Execute move
      chessGame.makeMove(move);
      gameDAO.updateGame(game);
      System.out.println("✅ [WEBSOCKET-MOVE] Move successful");

      // Broadcast updated game state
      LoadGame loadGame = new LoadGame(chessGame);
      String loadGameJson = gson.toJson(loadGame);
      System.out.println("📤 [WEBSOCKET-MOVE] Broadcasting game state");
      broadcast(gameConnections.get(command.getGameID()), loadGameJson);

      // Broadcast move notification
      String moveNotification = String.format("%s moved from %s to %s",
              auth.username(), move.getStartPosition(), move.getEndPosition());
      System.out.println("📢 [WEBSOCKET-MOVE] Broadcasting: " + moveNotification);
      broadcastNotification(command.getGameID(), moveNotification, null);

      // Check game state
      ChessGame.TeamColor currentTeam = chessGame.getTeamTurn();
      if (chessGame.isInCheckmate(currentTeam)) {
        broadcastNotification(command.getGameID(),
                String.format("Checkmate! %s wins!", currentTeam == ChessGame.TeamColor.WHITE ? "Black" : "White"),
                null);
      } else if (chessGame.isInCheck(currentTeam)) {
        broadcastNotification(command.getGameID(), currentTeam + " is in check!", null);
      }
    } catch (InvalidMoveException e) {
      System.out.println("❌ [WEBSOCKET-MOVE] Invalid move: " + e.getMessage());
      sendError(session, "Error: invalid move");
    }
  }

  private void handleResign(Session session, UserGameCommand command, AuthData auth, GameData game) throws Exception {
    System.out.println("🔄 [WEBSOCKET-RESIGN] Processing resign for user: " + auth.username());

    if (!auth.username().equals(game.whiteUsername()) && !auth.username().equals(game.blackUsername())) {
      System.out.println("❌ [WEBSOCKET-RESIGN] Not a player in the game");
      sendError(session, "Error: only players can resign");
      return;
    }

    System.out.println("📢 [WEBSOCKET-RESIGN] Broadcasting resignation");
    broadcastNotification(command.getGameID(), auth.username() + " resigned from the game", null);
  }

  private void handleLeave(Session session, UserGameCommand command) {
    System.out.println("🔄 [WEBSOCKET-LEAVE] Processing leave command");
    Map<Session, String> gameSessions = gameConnections.get(command.getGameID());
    if (gameSessions != null) {
      String username = gameSessions.remove(session);
      if (username != null) {
        System.out.println("📢 [WEBSOCKET-LEAVE] Broadcasting departure");
        broadcastNotification(command.getGameID(), username + " left the game", session);
      }
    }
  }

  private void sendError(Session session, String message) {
    try {
      System.out.println("❌ [WEBSOCKET-ERROR] Sending error: " + message);
      Error error = new Error(message);
      String errorJson = gson.toJson(error);
      session.getRemote().sendString(errorJson);
    } catch (Exception e) {
      System.err.println("❌ [WEBSOCKET-ERROR] Failed to send error: " + e.getMessage());
    }
  }

  private void broadcastNotification(int gameId, String message, Session exclude) {
    System.out.println("📢 [WEBSOCKET-BROADCAST] Notification to game " + gameId + ": " + message);
    Notification notification = new Notification(message);
    String jsonNotification = gson.toJson(notification);

    Map<Session, String> sessions = gameConnections.get(gameId);
    if (sessions != null) {
      for (Session session : sessions.keySet()) {
        if (session != exclude) {
          try {
            session.getRemote().sendString(jsonNotification);
            System.out.println("📤 [WEBSOCKET-BROADCAST] Sent to session: " + session.getRemoteAddress());
          } catch (Exception e) {
            System.err.println("❌ [WEBSOCKET-BROADCAST] Failed to send to session: " + e.getMessage());
          }
        }
      }
    }
  }

  private void broadcast(Map<Session, String> sessions, String message) {
    System.out.println("📢 [WEBSOCKET-BROADCAST] Broadcasting message to all sessions");
    if (sessions != null) {
      for (Session session : sessions.keySet()) {
        try {
          session.getRemote().sendString(message);
          System.out.println("📤 [WEBSOCKET-BROADCAST] Sent to session: " + session.getRemoteAddress());
        } catch (Exception e) {
          System.err.println("❌ [WEBSOCKET-BROADCAST] Failed to send: " + e.getMessage());
        }
      }
    }
  }

  @OnWebSocketClose
  @Override
  public void onWebSocketClose(int statusCode, String reason) {
    Session session = getSession();
    System.out.println("\n🔴 [WEBSOCKET-CLOSE] Connection closing for session " +
            (session != null ? session.hashCode() : "null"));
    System.out.println("🔴 [WEBSOCKET-CLOSE] Status: " + statusCode + ", Reason: " + reason);

    if (session != null) {
      // Check if there was a pending message that wasn't processed
      String pendingMessage = pendingMessages.remove(session);
      if (pendingMessage != null) {
        System.out.println("⚠️ [WEBSOCKET-CLOSE] Found unprocessed message: " + pendingMessage);
      }

      // Clean up connections
      cleanupSession(session);
    }
    super.onWebSocketClose(statusCode, reason);
  }

  @OnWebSocketError
  @Override
  public void onWebSocketError(Throwable cause) {
    System.err.println("⛔ [WEBSOCKET-ERROR] Error occurred:");
    cause.printStackTrace();
    super.onWebSocketError(cause);
  }
}