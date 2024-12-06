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
  }

  @OnWebSocketMessage
  public void onWebSocketMessage(String message) {
    try {
      UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
      AuthData auth = authDAO.getAuth(command.getAuthToken());

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
      sendError(getSession(), "Error: " + e.getMessage());
    }
  }

  private void handleConnect(Session session, String originalMessage, AuthData auth) throws Exception {
    // Parse specific command type (JoinPlayer vs JoinObserver)
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

    // Add to game connections
    gameConnections.computeIfAbsent(command.getGameID(), k -> new ConcurrentHashMap<>())
            .put(session, auth.username());

    // Send LOAD_GAME to connecting client
    LoadGame loadGameMessage = new LoadGame(game.game());
    session.getRemote().sendString(gson.toJson(loadGameMessage));

    // Send notification to other clients
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
    if (!(command instanceof MakeMove moveCommand)) {
      sendError(session, "Error: invalid move command");
      return;
    }

    GameData game = gameDAO.getGame(command.getGameID());
    if (game == null) {
      sendError(session, "Error: game not found");
      return;
    }

    try {
      game.game().makeMove(moveCommand.getMove());
      gameDAO.updateGame(game);

      // Send updated game state to all clients
      LoadGame loadGame = new LoadGame(game.game());
      broadcast(gameConnections.get(command.getGameID()), gson.toJson(loadGame));

      // Send move notification
      String moveNotification = String.format("%s moved from %s to %s",
              auth.username(),
              moveCommand.getMove().getStartPosition(),
              moveCommand.getMove().getEndPosition());
      broadcastNotification(command.getGameID(), moveNotification, null);

      // Check for game state changes
      ChessGame.TeamColor currentTeam = game.game().getTeamTurn();
      if (game.game().isInCheckmate(currentTeam)) {
        broadcastNotification(command.getGameID(), currentTeam + " is in checkmate!", null);
      } else if (game.game().isInCheck(currentTeam)) {
        broadcastNotification(command.getGameID(), currentTeam + " is in check!", null);
      }
    } catch (InvalidMoveException e) {
      sendError(session, "Error: invalid move");
    }
  }

  private void handleResign(Session session, UserGameCommand command, AuthData auth) throws Exception {
    GameData game = gameDAO.getGame(command.getGameID());
    if (game == null) {
      sendError(session, "Error: game not found");
      return;
    }

    String notification = String.format("%s resigned from the game", auth.username());
    broadcastNotification(command.getGameID(), notification, null);
  }

  private void handleLeave(Session session, UserGameCommand command) {
    Map<Session, String> gameSession = gameConnections.get(command.getGameID());
    if (gameSession != null) {
      String username = gameSession.remove(session);
      if (username != null) {
        broadcastNotification(command.getGameID(), username + " left the game", session);
      }
    }
  }

  private void sendError(Session session, String message) {
    try {
      Error error = new Error(message);
      session.getRemote().sendString(gson.toJson(error));
    } catch (Exception e) {
      System.err.println("Error sending error message: " + e.getMessage());
    }
  }

  private void broadcastNotification(int gameId, String message, Session exclude) {
    Notification notification = new Notification(message);
    String jsonNotification = gson.toJson(notification);

    Map<Session, String> sessions = gameConnections.get(gameId);
    if (sessions != null) {
      for (Session session : sessions.keySet()) {
        if (session != exclude) {
          try {
            session.getRemote().sendString(jsonNotification);
          } catch (Exception e) {
            System.err.println("Error broadcasting message: " + e.getMessage());
          }
        }
      }
    }
  }

  private void broadcast(Map<Session, String> sessions, String message) {
    if (sessions != null) {
      for (Session session : sessions.keySet()) {
        try {
          session.getRemote().sendString(message);
        } catch (Exception e) {
          System.err.println("Error broadcasting message: " + e.getMessage());
        }
      }
    }
  }

  @OnWebSocketConnect
  @Override
  public void onWebSocketConnect(Session session) {
    super.onWebSocketConnect(session);
  }

  @OnWebSocketClose
  @Override
  public void onWebSocketClose(int statusCode, String reason) {
    Session session = getSession();
    if (session != null) {
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
  @Override
  public void onWebSocketError(Throwable cause) {
    System.err.println("WebSocket Error: " + cause.getMessage());
    super.onWebSocketError(cause);
  }
}