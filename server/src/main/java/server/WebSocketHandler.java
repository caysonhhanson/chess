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


  @Override
  public void onWebSocketText(String message) {
    System.out.println("\n⚡ [DEBUG] onWebSocketText triggered with message: " + message);
    try {
      UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
      System.out.println("🔄 [WS-MESSAGE] Parsed command type: " + command.getCommandType());

      AuthData auth = Server.authDAO.getAuth(command.getAuthToken());
      if (auth == null) {
        System.out.println("❌ [WS-MESSAGE] Invalid auth token");
        sendError(getSession(), "Error: unauthorized");
        return;
      }

      GameData game = null;
      try {
        game = Server.gameDAO.getGame(command.getGameID());
        if (game == null) {
          System.out.println("❌ [WS-MESSAGE] Game not found: " + command.getGameID());
          sendError(getSession(), "Error: game not found");
          return;
        }
      } catch (Exception e) {
        System.out.println("❌ [WS-MESSAGE] Error retrieving game: " + e.getMessage());
        sendError(getSession(), "Error: game not found");
        return;
      }

      switch (command.getCommandType()) {
        case CONNECT -> {
          System.out.println("🔄 [WS-MESSAGE] Processing CONNECT command");
          handleConnect(getSession(), command, auth, game);
        }
        case MAKE_MOVE -> {
          System.out.println("🔄 [WS-MESSAGE] Processing MAKE_MOVE command");
          handleMove(getSession(), command, auth, game);
        }
        case RESIGN -> {
          System.out.println("🔄 [WS-MESSAGE] Processing RESIGN command");
          handleResign(getSession(), command, auth, game);
        }
        case LEAVE -> {
          System.out.println("🔄 [WS-MESSAGE] Processing LEAVE command");
          handleLeave(getSession(), command, auth);
        }
        default -> {
          System.out.println("❌ [WS-MESSAGE] Unknown command type: " + command.getCommandType());
          sendError(getSession(), "Error: unknown command type");
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [WS-MESSAGE] Error processing message: ");
      e.printStackTrace();
      sendError(getSession(), "Error: " + e.getMessage());
    }
  }

  @OnWebSocketConnect
  @Override
  public void onWebSocketConnect(Session session) {
    System.out.println("\n⚡ [DEBUG] onWebSocketConnect triggered");
    System.out.println("\n🔌 [WS-HANDLER] New WebSocket connection from: " + session.getRemoteAddress());
    super.onWebSocketConnect(session);
    System.out.println("🔌 [WS-HANDLER] Session initialized with ID: " + session.hashCode());
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
      chessGame.makeMove(moveCommand.getMove());
      Server.gameDAO.updateGame(game);

      Map<Session, String> gameSessions = gameConnections.get(command.getGameID());
      if (gameSessions != null) {
        LoadGame loadGame = new LoadGame(chessGame);
        String loadGameJson = gson.toJson(loadGame);

        String moveNotification = String.format("%s moved from %s to %s",
                auth.username(),
                moveCommand.getMove().getStartPosition(),
                moveCommand.getMove().getEndPosition());
        Notification notification = new Notification(moveNotification);
        String notificationJson = gson.toJson(notification);

        for (Map.Entry<Session, String> entry : gameSessions.entrySet()) {
          Session clientSession = entry.getKey();
          String username = entry.getValue();

          if (clientSession.isOpen()) {
            // Always send game state update
            clientSession.getRemote().sendString(loadGameJson);

            // Send notification based on team and observer status
            if (isWhite) {
              // If white moved, send to black player and observers
              if (username.equals(game.blackUsername()) ||
                      (!username.equals(game.whiteUsername()) && !username.equals(game.blackUsername()))) {
                clientSession.getRemote().sendString(notificationJson);
              }
            } else {
              // If black moved, send to white player and observers
              if (username.equals(game.whiteUsername()) ||
                      (!username.equals(game.whiteUsername()) && !username.equals(game.blackUsername()))) {
                clientSession.getRemote().sendString(notificationJson);
              }
            }
          }
        }
      }

      ChessGame.TeamColor currentTeam = chessGame.getTeamTurn();
      if (chessGame.isInCheckmate(currentTeam)) {
        ChessGame.TeamColor winner = (currentTeam == ChessGame.TeamColor.WHITE) ?
                ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
        broadcastNotification(command.getGameID(),
                String.format("Checkmate! %s wins!", winner),
                null);  // Send to everyone
      } else if (chessGame.isInCheck(currentTeam)) {
        broadcastNotification(command.getGameID(),
                String.format("%s is in check!", currentTeam),
                null);  // Send to everyone
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


  private void sendError(Session session, String message) {
    try {
      Error error = new Error(message);
      session.getRemote().sendString(gson.toJson(error));
    } catch (Exception e) {
      System.err.println("❌ [WS-ERROR] Failed to send error: " + e.getMessage());
    }
  }

  private void broadcastNotification(int gameId, String message, Session exclude) {
    try {
      Notification notification = new Notification(message);
      String jsonNotification = gson.toJson(notification);
      Map<Session, String> sessions = gameConnections.get(gameId);
      if (sessions != null) {
        for (Session session : sessions.keySet()) {
          if (session != exclude && session.isOpen()) {
            try {
              session.getRemote().sendString(jsonNotification);
            } catch (Exception e) {
              System.err.println("❌ [WS-BROADCAST] Failed to send to session: " + e.getMessage());
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [WS-BROADCAST] Failed to broadcast notification: " + e.getMessage());
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
    System.out.println("\n⚡ [DEBUG] onWebSocketClose triggered");
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
    System.out.println("\n⚡ [DEBUG] onWebSocketError triggered");
    System.err.println("\n❌ [WS-ERROR] WebSocket error occurred:");
    cause.printStackTrace();
  }
}