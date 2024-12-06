package server;

import chess.ChessGame;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.*;
import model.AuthData;
import model.GameData;
import websocket.commands.*;
import websocket.messages.*;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws")
public class WebSocketHandler {
  private static final Map<Integer, Map<Session, String>> gameConnections = new ConcurrentHashMap<>();
  private final GameDAO gameDAO;
  private final AuthDAO authDAO;
  private final Gson gson = new Gson();

  public WebSocketHandler(GameDAO gameDAO, AuthDAO authDAO) {
    this.gameDAO = gameDAO;
    this.authDAO = authDAO;
  }

  @OnOpen
  public void onOpen(Session session) {
    // Connection established
  }

  @OnMessage
  public void onMessage(String message, Session session) {
    try {
      UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
      AuthData auth = authDAO.getAuth(command.getAuthToken());

      if (auth == null) {
        sendError(session, "Error: unauthorized");
        return;
      }

      switch (command.getCommandType()) {
        case CONNECT -> handleConnect(session, command, auth);
        case MAKE_MOVE -> handleMove(session, command, auth);
        case RESIGN -> handleResign(session, command, auth);
        case LEAVE -> handleLeave(session, command);
      }
    } catch (Exception e) {
      sendError(session, "Error: " + e.getMessage());
    }
  }

  private void handleConnect(Session session, UserGameCommand command, AuthData auth) throws Exception {
    GameData game = gameDAO.getGame(command.getGameID());
    if (game == null) {
      sendError(session, "Error: game not found");
      return;
    }

    gameConnections.computeIfAbsent(command.getGameID(), k -> new ConcurrentHashMap<>())
            .put(session, auth.username());

    sendLoadGame(session, game.game());

    String role = command instanceof JoinPlayer ?
            "as " + ((JoinPlayer) command).getPlayerColor() + " player" :
            "as an observer";

    broadcastNotification(command.getGameID(),
            String.format("%s joined the game %s", auth.username(), role));
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

    ChessGame.TeamColor currentTurn = game.game().getTeamTurn();
    String expectedPlayer = currentTurn == ChessGame.TeamColor.WHITE ?
            game.whiteUsername() : game.blackUsername();

    if (!auth.username().equals(expectedPlayer)) {
      sendError(session, "Error: not your turn");
      return;
    }

    try {
      game.game().makeMove(moveCommand.getMove());
      gameDAO.updateGame(game);

      broadcastGameUpdate(command.getGameID(), game.game());
      broadcastNotification(command.getGameID(),
              String.format("%s moved %s to %s",
                      auth.username(),
                      moveCommand.getMove().getStartPosition(),
                      moveCommand.getMove().getEndPosition()));

      checkGameState(game, command.getGameID());
    } catch (InvalidMoveException e) {
      sendError(session, "Error: invalid move");
    }
  }

  private void checkGameState(GameData game, int gameId) {
    ChessGame.TeamColor currentTeam = game.game().getTeamTurn();
    if (game.game().isInCheck(currentTeam)) {
      if (game.game().isInCheckmate(currentTeam)) {
        broadcastNotification(gameId, currentTeam + " is in checkmate!");
      } else {
        broadcastNotification(gameId, currentTeam + " is in check!");
      }
    } else if (game.game().isInStalemate(currentTeam)) {
      broadcastNotification(gameId, "Game is in stalemate!");
    }
  }

  private void handleResign(Session session, UserGameCommand command, AuthData auth) throws Exception {
    GameData game = gameDAO.getGame(command.getGameID());
    if (game == null) {
      sendError(session, "Error: game not found");
      return;
    }

    if (!auth.username().equals(game.whiteUsername()) &&
            !auth.username().equals(game.blackUsername())) {
      sendError(session, "Error: not a player in this game");
      return;
    }

    broadcastNotification(command.getGameID(), auth.username() + " has resigned");
  }

  private void handleLeave(Session session, UserGameCommand command) {
    Map<Session, String> gameSession = gameConnections.get(command.getGameID());
    if (gameSession != null) {
      String username = gameSession.remove(session);
      if (username != null) {
        broadcastNotification(command.getGameID(), username + " left the game");
      }
    }
  }

  private void sendError(Session session, String message) {
    websocket.messages.Error error = new websocket.messages.Error(message);
    try {
      session.getBasicRemote().sendText(gson.toJson(error));
    } catch (Exception e) {
      System.err.println("Error sending error message: " + e.getMessage());
    }
  }

  private void sendLoadGame(Session session, ChessGame game) {
    LoadGame loadGame = new LoadGame(game);
    try {
      session.getBasicRemote().sendText(gson.toJson(loadGame));
    } catch (Exception e) {
      System.err.println("Error sending game state: " + e.getMessage());
    }
  }

  private void broadcastGameUpdate(int gameId, ChessGame game) {
    LoadGame loadGame = new LoadGame(game);
    broadcast(gameId, gson.toJson(loadGame));
  }

  private void broadcastNotification(int gameId, String message) {
    Notification notification = new Notification(message);
    broadcast(gameId, gson.toJson(notification));
  }

  private void broadcast(int gameId, String message) {
    Map<Session, String> gameSessions = gameConnections.get(gameId);
    if (gameSessions != null) {
      for (Session session : gameSessions.keySet()) {
        try {
          session.getBasicRemote().sendText(message);
        } catch (Exception e) {
          System.err.println("Error broadcasting message: " + e.getMessage());
        }
      }
    }
  }

  @OnClose
  public void onClose(Session session) {
    gameConnections.values().forEach(sessions -> sessions.remove(session));
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    System.err.println("WebSocket error: " + throwable.getMessage());
  }
}