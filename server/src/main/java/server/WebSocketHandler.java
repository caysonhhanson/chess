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
        case CONNECT -> handleConnect(getSession(), command, auth);
        case MAKE_MOVE -> handleMove(getSession(), command, auth);
        case RESIGN -> handleResign(getSession(), command, auth);
        case LEAVE -> handleLeave(getSession(), command);
      }
    } catch (Exception e) {
      sendError(getSession(), "Error: " + e.getMessage());
    }
  }

  @OnWebSocketError
  @Override
  public void onWebSocketError(Throwable cause) {
    System.err.println("WebSocket Error: " + cause.getMessage());
    super.onWebSocketError(cause);
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

    if (isGameOver(game.game())) {
      sendError(session, "Error: game is already over");
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
      ChessMove move = moveCommand.getMove();
      if (isPawnPromotion(game.game(), move)) {
        move = new ChessMove(move.getStartPosition(), move.getEndPosition(),
                ChessPiece.PieceType.QUEEN);
      }

      game.game().makeMove(move);
      gameDAO.updateGame(game);

      broadcastGameUpdate(command.getGameID(), game.game());
      broadcastNotification(command.getGameID(),
              String.format("%s moved %s to %s",
                      auth.username(),
                      move.getStartPosition(),
                      move.getEndPosition()));

      checkGameState(game, command.getGameID());
    } catch (InvalidMoveException e) {
      sendError(session, "Error: invalid move");
    }
  }

  private boolean isGameOver(ChessGame game) {
    ChessGame.TeamColor currentTeam = game.getTeamTurn();
    return game.isInCheckmate(currentTeam) || game.isInStalemate(currentTeam);
  }

  private boolean isPawnPromotion(ChessGame game, ChessMove move) {
    ChessPosition start = move.getStartPosition();
    ChessPosition end = move.getEndPosition();
    ChessPiece piece = game.getBoard().getPiece(start);

    return piece != null &&
            piece.getPieceType() == ChessPiece.PieceType.PAWN &&
            ((piece.getTeamColor() == ChessGame.TeamColor.WHITE && end.getRow() == 8) ||
                    (piece.getTeamColor() == ChessGame.TeamColor.BLACK && end.getRow() == 1));
  }

  private void checkGameState(GameData game, int gameId) {
    ChessGame.TeamColor currentTeam = game.game().getTeamTurn();
    if (game.game().isInCheckmate(currentTeam)) {
      broadcastNotification(gameId, currentTeam + " is in checkmate!");
    } else if (game.game().isInCheck(currentTeam)) {
      broadcastNotification(gameId, currentTeam + " is in check!");
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
    try {
      session.getRemote().sendString(gson.toJson(new Error(message)));
    } catch (Exception e) {
      System.err.println("Error sending error message: " + e.getMessage());
    }
  }

  private void sendLoadGame(Session session, ChessGame game) {
    try {
      session.getRemote().sendString(gson.toJson(new LoadGame(game)));
    } catch (Exception e) {
      System.err.println("Error sending game state: " + e.getMessage());
    }
  }

  private void broadcastGameUpdate(int gameId, ChessGame game) {
    LoadGame loadGame = new LoadGame(game);
    broadcast(gameConnections.get(gameId), gson.toJson(loadGame));
  }

  private void broadcastNotification(int gameId, String message) {
    Notification notification = new Notification(message);
    broadcast(gameConnections.get(gameId), gson.toJson(notification));
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
}