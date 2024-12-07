package server;

import chess.*;
import com.google.gson.*;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.*;
import websocket.commands.*;
import websocket.messages.*;
import websocket.messages.Error;

import javax.websocket.OnOpen;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler extends WebSocketAdapter {
  private static final Map<Integer, Map<Session, String>> gameConnections = new ConcurrentHashMap<>();
  private final Gson gson;

  public WebSocketHandler() {
    this.gson = new GsonBuilder()
            .registerTypeAdapter(UserGameCommand.class, (JsonDeserializer<UserGameCommand>) (json, typeOfT, context) -> {
              JsonObject obj = json.getAsJsonObject();
              String commandType = obj.get("commandType").getAsString();
              String authToken = obj.get("authToken").getAsString();
              Integer gameID = obj.get("gameID").getAsInt();

              return switch (commandType) {
                case "MAKE_MOVE" -> context.deserialize(json, MakeMove.class);
                case "CONNECT" -> new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
                case "LEAVE" -> new Leave(authToken, gameID);
                case "RESIGN" -> new Resign(authToken, gameID);
                default -> throw new JsonParseException("Unknown command type: " + commandType);
              };
            }).create();
  }

  @OnOpen
  public void onOpen(Session session){
    System.out.println("Connection opened" + session.getRemoteAddress().getHostName());
  }


  @OnWebSocketConnect
  @Override
  public void onWebSocketConnect(Session session) {
    System.out.println("\n🔌 [WS-HANDLER] New WebSocket connection from: " + session.getRemoteAddress());
    super.onWebSocketConnect(session);
    session.setIdleTimeout(300000);
    System.out.println("🔌 [WS-HANDLER] Session initialized with ID: " + session.hashCode());

    // Send initial LOAD_GAME message
    LoadGame loadGame = new LoadGame(new ChessGame());
    try {
      String gameJson = gson.toJson(loadGame);
      session.getRemote().sendString(gameJson);
      System.out.println("✅ [WS-HANDLER] Sent initial LOAD_GAME message");
    } catch (Exception e) {
      System.err.println("❌ [WS-HANDLER] Failed to send initial game state: " + e.getMessage());
    }
  }

  @OnWebSocketMessage
  public void onWebSocketMessage(Session session, String message) {
    System.out.println("\n📥 [WS-MESSAGE] Received message: " + message);
    try {
      UserGameCommand command = gson.fromJson(message, UserGameCommand.class);

      if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {
        // Store connection in game connections map
        Map<Session, String> gameSessions = gameConnections.computeIfAbsent(
                command.getGameID(),
                k -> new ConcurrentHashMap<>()
        );
        gameSessions.put(session, command.getAuthToken());
        System.out.println("✅ [WS-MESSAGE] Added connection to game " + command.getGameID());

        // Send LOAD_GAME to the connecting player
        LoadGame loadGame = new LoadGame(new ChessGame());
        String loadGameJson = gson.toJson(loadGame);
        session.getRemote().sendString(loadGameJson);

        // Send notification to other players
        if (gameSessions.size() > 1) {
          Notification notification = new Notification("A player has connected");
          String notificationJson = gson.toJson(notification);

          for (Session existingSession : gameSessions.keySet()) {
            if (existingSession != session && existingSession.isOpen()) {
              try {
                existingSession.getRemote().sendString(notificationJson);
                System.out.println("✅ [WS-MESSAGE] Sent notification to session " + existingSession.hashCode());
              } catch (Exception e) {
                System.err.println("❌ [WS-MESSAGE] Failed to send notification: " + e.getMessage());
              }
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [WS-MESSAGE] Error processing message: " + e.getMessage());
      e.printStackTrace();
      sendError(session, "Error: " + e.getMessage());
    }
  }

  private void handleConnect(Session session, UserGameCommand command, AuthData auth, GameData game) {
    try {
      System.out.println("\n🔄 [CONNECT] Starting connection for user: " + auth.username());
      System.out.println("🔄 [CONNECT] Game ID: " + command.getGameID());
      System.out.println("🔄 [CONNECT] Game state: " + (game != null ? "found" : "null"));

      Map<Session, String> gameSessions = gameConnections.computeIfAbsent(
              command.getGameID(),
              k -> new ConcurrentHashMap<>()
      );
      gameSessions.put(session, auth.username());
      System.out.println("✅ [CONNECT] Added to game connections. Current players in game: " + gameSessions.size());


      LoadGame loadGameMessage = new LoadGame(game.game());
      String loadGameJson = gson.toJson(loadGameMessage);
      System.out.println("📤 [CONNECT] Sending LOAD_GAME message: " + loadGameJson);

      try {
        session.getRemote().sendString(loadGameJson);
        System.out.println("✅ [CONNECT] LOAD_GAME message sent successfully");
      } catch (Exception e) {
        System.err.println("❌ [CONNECT] Failed to send LOAD_GAME message: " + e.getMessage());
        e.printStackTrace();
        throw e;
      }

      // Determine player type and create notification
      String notificationMessage;
      if (auth.username().equals(game.whiteUsername())) {
        notificationMessage = String.format("%s joined as WHITE player", auth.username());
        System.out.println("👤 [CONNECT] User is WHITE player");
      } else if (auth.username().equals(game.blackUsername())) {
        notificationMessage = String.format("%s joined as BLACK player", auth.username());
        System.out.println("👤 [CONNECT] User is BLACK player");
      } else {
        notificationMessage = String.format("%s joined as an observer", auth.username());
        System.out.println("👀 [CONNECT] User is OBSERVER");
      }

      // Broadcast notification
      System.out.println("📢 [CONNECT] Broadcasting notification: " + notificationMessage);
      try {
        broadcastNotification(command.getGameID(), notificationMessage, session);
        System.out.println("✅ [CONNECT] Notification broadcast complete");
      } catch (Exception e) {
        System.err.println("❌ [CONNECT] Failed to broadcast notification: " + e.getMessage());
        e.printStackTrace();
        throw e;
      }

      System.out.println("✅ [CONNECT] Connection handling completed successfully\n");

    } catch (Exception e) {
      System.err.println("❌ [CONNECT] Error during connect: " + e.getMessage());
      e.printStackTrace();
      sendError(session, "Error during connect: " + e.getMessage());
    }
  }

  private void handleMove(Session session, UserGameCommand command, AuthData auth, GameData game) {
    if (!(command instanceof MakeMove moveCommand)) {
      sendError(session, "Error: invalid move command");
      return;
    }

    ChessGame chessGame = game.game();

    // Validate player's turn
    boolean isWhite = auth.username().equals(game.whiteUsername());
    boolean isBlack = auth.username().equals(game.blackUsername());

    if (!isWhite && !isBlack) {
      sendError(session, "Error: not a player in the game");
      return;
    }

    if ((chessGame.getTeamTurn() == ChessGame.TeamColor.WHITE && !isWhite) ||
            (chessGame.getTeamTurn() == ChessGame.TeamColor.BLACK && !isBlack)) {
      sendError(session, "Error: not your turn");
      return;
    }

    try {
      // Make the move
      chessGame.makeMove(moveCommand.getMove());
      Server.gameDAO.updateGame(game);

      // Broadcast updated game state to all players
      LoadGame loadGame = new LoadGame(chessGame);
      broadcast(gameConnections.get(command.getGameID()), gson.toJson(loadGame));

      // Send move notification
      String moveNotification = String.format("%s moved from %s to %s",
              auth.username(),
              moveCommand.getMove().getStartPosition(),
              moveCommand.getMove().getEndPosition());
      broadcastNotification(command.getGameID(), moveNotification, null);

      // Check for checkmate or check
      ChessGame.TeamColor currentTeam = chessGame.getTeamTurn();
      if (chessGame.isInCheckmate(currentTeam)) {
        ChessGame.TeamColor winner = (currentTeam == ChessGame.TeamColor.WHITE) ?
                ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
        broadcastNotification(command.getGameID(),
                String.format("Checkmate! %s wins!", winner),
                null);
      } else if (chessGame.isInCheck(currentTeam)) {
        broadcastNotification(command.getGameID(),
                String.format("%s is in check!", currentTeam),
                null);
      }
    } catch (InvalidMoveException e) {
      sendError(session, "Error: invalid move");
    } catch (Exception e) {
      sendError(session, "Error: " + e.getMessage());
    }
  }

  private void handleResign(Session session, UserGameCommand command, AuthData auth, GameData game) {
    if (!auth.username().equals(game.whiteUsername()) && !auth.username().equals(game.blackUsername())) {
      sendError(session, "Error: only players can resign");
      return;
    }

    broadcastNotification(command.getGameID(),
            String.format("%s resigned from the game", auth.username()),
            null);
  }

  private void handleLeave(Session session, UserGameCommand command, AuthData auth) {
    Map<Session, String> gameSessions = gameConnections.get(command.getGameID());
    if (gameSessions != null) {
      gameSessions.remove(session);
      broadcastNotification(command.getGameID(),
              String.format("%s left the game", auth.username()),
              session);
    }
  }

  private void sendMessage(Session session, ServerMessage message) {
    try {
      String gameJson = gson.toJson(message);
      System.out.println(gameJson);
      session.getRemote().sendString(gameJson);
      System.out.println("Game Sent");
    } catch (Exception e) {
      System.err.println("Failed  to send Game" + e.getMessage());
      e.printStackTrace();
    }
  }

  private void sendError(Session session, String message) {
    try {
      Error error = new Error(message);
      String errorJson = gson.toJson(error);
      System.out.println("📤 [ERROR] Sending error message: " + errorJson);
      session.getRemote().sendString(errorJson);
      System.out.println("✅ [ERROR] Error message sent");
    } catch (Exception e) {
      System.err.println("❌ [ERROR] Failed to send error message: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void broadcastNotification(int gameId, String message, Session exclude) {
    try {
      Map<Session, String> sessions = gameConnections.get(gameId);
      if (sessions == null) {
        System.out.println("ℹ️ [BROADCAST] No sessions found for game " + gameId);
        return;
      }

      Notification notification = new Notification(message);
      String notificationJson = gson.toJson(notification);
      System.out.println("📢 [BROADCAST] Broadcasting to " + (sessions.size() - 1) + " other sessions: " + notificationJson);

      for (Session session : sessions.keySet()) {
        if (session != exclude && session.isOpen()) {
          try {
            session.getRemote().sendString(notificationJson);
            System.out.println("✅ [BROADCAST] Sent to session: " + session.hashCode());
          } catch (Exception e) {
            System.err.println("❌ [BROADCAST] Failed to send to session " + session.hashCode() + ": " + e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [BROADCAST] Error during broadcast: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void broadcast(Map<Session, String> sessions, String message) {
    if (sessions != null) {
      for (Session session : sessions.keySet()) {
        try {
          if (session.isOpen()) {
            session.getRemote().sendString(message);
          }
        } catch (Exception e) {
          System.err.println("❌ [WS-BROADCAST] Failed to broadcast: " + e.getMessage());
        }
      }
    }
  }

  @OnWebSocketClose
  public void onWebSocketClose(int statusCode, String reason) {
    Session session = getSession();
    System.out.println("\n🔴 [WS-CLOSE] Connection closing for session " + session.hashCode());
    System.out.println("🔴 [WS-CLOSE] Status: " + statusCode + ", Reason: " + reason);

    if (session != null) {
      // Clean up all game connections for this session
      gameConnections.values().forEach(sessions -> {
        String username = sessions.remove(session);
        if (username != null) {
          broadcast(sessions, gson.toJson(new Notification(username + " disconnected")));
        }
      });
    }
    super.onWebSocketClose(statusCode, reason);
  }

  @OnWebSocketError
  public void onWebSocketError(Throwable cause) {
    System.err.println("\n❌ [WS-ERROR] WebSocket error occurred:");
    cause.printStackTrace();
  }
}