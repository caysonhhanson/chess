package server;

import chess.*;
import com.google.gson.Gson;
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
  private final Gson gson = new Gson();

  public WebSocketHandler(GameDAO gameDAO, AuthDAO authDAO) {
    this.gameDAO = gameDAO;
    this.authDAO = authDAO;
    System.out.println("WebSocketHandler initialized");
  }

  @OnWebSocketMessage
  public void onWebSocketMessage(String message) {
    System.out.println("\n=== Received WebSocket Message ===");
    System.out.println("Raw message: " + message);

    try {
      UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
      System.out.println("Command type: " + command.getCommandType());
      System.out.println("Game ID: " + command.getGameID());
      System.out.println("Auth token: " + command.getAuthToken());

      AuthData auth = authDAO.getAuth(command.getAuthToken());
      System.out.println("Auth valid: " + (auth != null));

      if (auth == null) {
        sendError(getSession(), "Error: unauthorized");
        return;
      }

      switch (command.getCommandType()) {
        case CONNECT -> handleConnect(getSession(), message, auth);
        case MAKE_MOVE -> handleMove(getSession(), command, auth);
        case RESIGN -> handleResign(getSession(), command, auth);
        case LEAVE -> handleLeave(getSession(), command);
      }
    } catch (Exception e) {
      System.out.println("Error processing message: ");
      e.printStackTrace();
      sendError(getSession(), "Error: " + e.getMessage());
    }
  }

  private void handleConnect(Session session, String originalMessage, AuthData auth) throws Exception {
    UserGameCommand command;
    if (originalMessage.contains("playerColor")) {
      command = gson.fromJson(originalMessage, JoinPlayer.class);
    } else {
      command = gson.fromJson(originalMessage, JoinObserver.class);
    }

    GameData game = gameDAO.getGame(command.getGameID());
    if (game == null) {
      sendError(session, "Error: game not found");
      return;
    }

    // Add debugging here
    System.out.println("Original message: " + originalMessage);
    System.out.println("Parsed command: " + gson.toJson(command));
    System.out.println("Game found: " + gson.toJson(game));

    // Add to connections before sending messages
    gameConnections.computeIfAbsent(command.getGameID(), k -> new ConcurrentHashMap<>())
            .put(session, auth.username());

    // Send load game first
    LoadGame loadGameMessage = new LoadGame(game.game());
    String loadGameJson = gson.toJson(loadGameMessage);
    System.out.println("Sending LOAD_GAME: " + loadGameJson);
    session.getRemote().sendString(loadGameJson);

    // Then broadcast notification
    String notificationMessage;
    if (command instanceof JoinPlayer) {
      String color = ((JoinPlayer) command).getPlayerColor();
      notificationMessage = String.format("%s joined as %s player", auth.username(), color);
    } else {
      notificationMessage = String.format("%s joined as an observer", auth.username());
    }
    broadcastNotification(command.getGameID(), notificationMessage, session);
  }

  private void handleMove(Session session, UserGameCommand command, AuthData auth) throws Exception {
    System.out.println("\n=== Handling Move ===");
    if (!(command instanceof MakeMove moveCommand)) {
      System.out.println("Invalid move command type");
      sendError(session, "Error: invalid move command");
      return;
    }

    GameData game = gameDAO.getGame(command.getGameID());
    System.out.println("Game found: " + (game != null));
    System.out.println("Move: " + gson.toJson(moveCommand.getMove()));

    if (game == null) {
      sendError(session, "Error: game not found");
      return;
    }

    try {
      game.game().makeMove(moveCommand.getMove());
      gameDAO.updateGame(game);
      System.out.println("Move successfully made");

      LoadGame loadGame = new LoadGame(game.game());
      String loadGameJson = gson.toJson(loadGame);
      System.out.println("Broadcasting updated game state: " + loadGameJson);
      broadcast(gameConnections.get(command.getGameID()), loadGameJson);

      String moveNotification = String.format("%s moved from %s to %s",
              auth.username(),
              moveCommand.getMove().getStartPosition(),
              moveCommand.getMove().getEndPosition());
      System.out.println("Broadcasting move notification: " + moveNotification);
      broadcastNotification(command.getGameID(), moveNotification, null);

      ChessGame.TeamColor currentTeam = game.game().getTeamTurn();
      if (game.game().isInCheckmate(currentTeam)) {
        broadcastNotification(command.getGameID(), currentTeam + " is in checkmate!", null);
      } else if (game.game().isInCheck(currentTeam)) {
        broadcastNotification(command.getGameID(), currentTeam + " is in check!", null);
      }
    } catch (InvalidMoveException e) {
      System.out.println("Invalid move attempted: " + e.getMessage());
      sendError(session, "Error: invalid move");
    }
  }

  private void handleResign(Session session, UserGameCommand command, AuthData auth) throws Exception {
    System.out.println("\n=== Handling Resign ===");
    GameData game = gameDAO.getGame(command.getGameID());
    System.out.println("Game found: " + (game != null));

    if (game == null) {
      sendError(session, "Error: game not found");
      return;
    }

    String notification = String.format("%s resigned from the game", auth.username());
    System.out.println("Broadcasting resignation: " + notification);
    broadcastNotification(command.getGameID(), notification, null);
  }

  private void handleLeave(Session session, UserGameCommand command) {
    System.out.println("\n=== Handling Leave ===");
    Map<Session, String> gameSession = gameConnections.get(command.getGameID());
    if (gameSession != null) {
      String username = gameSession.remove(session);
      System.out.println("User " + username + " leaving game " + command.getGameID());
      if (username != null) {
        broadcastNotification(command.getGameID(), username + " left the game", session);
      }
    }
  }

  private void sendError(Session session, String message) {
    try {
      System.out.println("Sending error: " + message);
      Error error = new Error(message);
      session.getRemote().sendString(gson.toJson(error));
    } catch (Exception e) {
      System.err.println("Error sending error message: " + e.getMessage());
    }
  }

  private void broadcastNotification(int gameId, String message, Session exclude) {
    System.out.println("Broadcasting notification to game " + gameId + ": " + message);
    Notification notification = new Notification(message);
    String jsonNotification = gson.toJson(notification);

    Map<Session, String> sessions = gameConnections.get(gameId);
    if (sessions != null) {
      for (Session session : sessions.keySet()) {
        if (session != exclude) {
          try {
            session.getRemote().sendString(jsonNotification);
            System.out.println("Sent notification to session: " + session.getRemoteAddress());
          } catch (Exception e) {
            System.err.println("Error broadcasting message: " + e.getMessage());
          }
        }
      }
    }
  }

  private void broadcast(Map<Session, String> sessions, String message) {
    System.out.println("Broadcasting message to all sessions: " + message);
    if (sessions != null) {
      for (Session session : sessions.keySet()) {
        try {
          session.getRemote().sendString(message);
          System.out.println("Sent to session: " + session.getRemoteAddress());
        } catch (Exception e) {
          System.err.println("Error broadcasting message: " + e.getMessage());
        }
      }
    }
  }

  @OnWebSocketConnect
  @Override
  public void onWebSocketConnect(Session session) {
    System.out.println("\n=== New WebSocket Connection ===");
    System.out.println("Session ID: " + session.hashCode());
    System.out.println("Remote: " + session.getRemoteAddress());
    super.onWebSocketConnect(session);
  }

  @OnWebSocketClose
  @Override
  public void onWebSocketClose(int statusCode, String reason) {
    System.out.println("\n=== WebSocket Closing ===");
    System.out.println("Status: " + statusCode + ", Reason: " + reason);
    Session session = getSession();
    if (session != null) {
      gameConnections.values().forEach(sessions -> {
        String username = sessions.remove(session);
        if (username != null) {
          System.out.println("User " + username + " disconnected");
          broadcast(sessions, gson.toJson(new Notification(username + " disconnected")));
        }
      });
    }
    super.onWebSocketClose(statusCode, reason);
  }

  @OnWebSocketError
  @Override
  public void onWebSocketError(Throwable cause) {
    System.err.println("\n=== WebSocket Error ===");
    System.err.println("Error: " + cause.getMessage());
    cause.printStackTrace();
    super.onWebSocketError(cause);
  }
}