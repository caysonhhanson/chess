package server;

import chess.ChessGame;
import chess.InvalidMoveException;
import com.google.gson.*;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import websocket.commands.Leave;
import websocket.commands.MakeMove;
import websocket.commands.Resign;
import websocket.commands.UserGameCommand;
import websocket.messages.Error;
import websocket.messages.LoadGame;
import websocket.messages.Notification;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler {
  private static final Map<Integer, Map<Session, String>> GAME_CONNECTIONS=new ConcurrentHashMap<>();
  private final Gson gson;

  public WebSocketHandler() {
    this.gson=new GsonBuilder().registerTypeAdapter(UserGameCommand.class, (JsonDeserializer<UserGameCommand>) (json, typeOfT, context) -> {
      JsonObject obj=json.getAsJsonObject();
      String commandType=obj.get("commandType").getAsString();
      String authToken=obj.get("authToken").getAsString();
      Integer gameID=obj.get("gameID").getAsInt();

      return switch (commandType) {
        case "MAKE_MOVE" -> context.deserialize(json, MakeMove.class);
        case "CONNECT" -> new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
        case "LEAVE" -> new Leave(authToken, gameID);
        case "RESIGN" -> new Resign(authToken, gameID);
        default -> throw new JsonParseException("Unknown command type: " + commandType);
      };
    }).create();
  }


  @OnWebSocketMessage
  public void onMessage(Session session, String message) {
    System.out.println("\n⚡ [DEBUG] onWebSocketText triggered with message: " + message);
    try {
      UserGameCommand command=gson.fromJson(message, UserGameCommand.class);
      System.out.println("🔄 [WS-MESSAGE] Parsed command type: " + command.getCommandType());

      AuthData auth=Server.authDAO.getAuth(command.getAuthToken());
      if (auth == null) {
        System.out.println("❌ [WS-MESSAGE] Invalid auth token: " + command.getAuthToken().toString());

        sendError(session, "Error: unauthorized");
        return;
      }

      GameData game=null;
      try {
        game=Server.gameDAO.getGame(command.getGameID());
        if (game == null) {
          System.out.println("❌ [WS-MESSAGE] Game not found: " + command.getGameID());
          sendError(session, "Error: game not found");
          return;
        }
      } catch (Exception e) {
        System.out.println("❌ [WS-MESSAGE] Error retrieving game: " + e.getMessage());
        sendError(session, "Error: game not found");
        return;
      }

      switch (command.getCommandType()) {
        case CONNECT -> {
          System.out.println("🔄 [WS-MESSAGE] Processing CONNECT command");
          handleConnect(session, command, auth, game);
        }
        case MAKE_MOVE -> {
          System.out.println("🔄 [WS-MESSAGE] Processing MAKE_MOVE command");
          handleMove(session, command, auth, game);
        }
        case RESIGN -> {
          System.out.println("🔄 [WS-MESSAGE] Processing RESIGN command");
          handleResign(session, command, auth, game);
        }
        case LEAVE -> {
          System.out.println("🔄 [WS-MESSAGE] Processing LEAVE command");
          handleLeave(session, command, auth, game);
        }
        default -> {
          System.out.println("❌ [WS-MESSAGE] Unknown command type: " + command.getCommandType());
          sendError(session, "Error: unknown command type");
        }
      }
    } catch (Exception e) {
      System.err.println("❌ [WS-MESSAGE] Error processing message: ");
      e.printStackTrace();
      sendError(session, "Error: " + e.getMessage());
    }
  }

  @OnWebSocketConnect
  public void onWebSocketConnect(Session session) {
    System.out.println("\n⚡ [DEBUG] onWebSocketConnect triggered");
    System.out.println("\n🔌 [WS-HANDLER] New WebSocket connection from: " + session.getRemoteAddress());

    System.out.println("🔌 [WS-HANDLER] Session initialized with ID: " + session.hashCode());
  }


  private void handleConnect(Session session, UserGameCommand command, AuthData auth, GameData game) {
    try {
      System.out.println("\n🔄 [CONNECT] Starting connection for user: " + auth.username());
      System.out.println("🔄 [CONNECT] Game ID: " + command.getGameID());
      System.out.println("🔄 [CONNECT] Game state: " + (game != null ? "found" : "null"));

      Map<Session, String> gameSessions=GAME_CONNECTIONS.computeIfAbsent(command.getGameID(), k -> new ConcurrentHashMap<>());
      gameSessions.put(session, auth.username());
      System.out.println("✅ [CONNECT] Added to game connections. Current players in game: " + gameSessions.size());


      LoadGame loadGameMessage=new LoadGame(game.game());
      String loadGameJson=gson.toJson(loadGameMessage);
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
        notificationMessage=String.format("%s joined as WHITE player", auth.username());
        System.out.println("👤 [CONNECT] User is WHITE player");
      } else if (auth.username().equals(game.blackUsername())) {
        notificationMessage=String.format("%s joined as BLACK player", auth.username());
        System.out.println("👤 [CONNECT] User is BLACK player");
      } else {
        notificationMessage=String.format("%s joined as an observer", auth.username());
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

    // Validate game state and player permissions
    if (!isValidMoveAttempt(session, auth, game)) {
      return;
    }

    ChessGame chessGame = game.game();
    try {
      // Make the move and update game state
      chessGame.makeMove(moveCommand.getMove());
      Server.gameDAO.updateGame(game);

      // Send updates to all connected clients
      sendGameUpdates(command.getGameID(), game, auth, moveCommand);

      // Check and handle game state changes
      handlePostMoveGameState(command.getGameID(), chessGame);

    } catch (InvalidMoveException e) {
      sendError(session, "Error: invalid move");
    } catch (Exception e) {
      sendError(session, "Error: " + e.getMessage());
    }
  }

  private boolean isValidMoveAttempt(Session session, AuthData auth, GameData game) {
    ChessGame chessGame = game.game();
    boolean isWhite = auth.username().equals(game.whiteUsername());
    boolean isBlack = auth.username().equals(game.blackUsername());

    // Check if user is a player
    if (!isWhite && !isBlack) {
      sendError(session, "Error: not a player in the game");
      return false;
    }

    // Check if game is already over
    if (chessGame.getTeamTurn() == ChessGame.TeamColor.RESIGNED) {
      sendError(session, "Game is Over");
      return false;
    }

    // Check if it's player's turn
    if (!isPlayersTurn(chessGame, isWhite, isBlack)) {
      sendError(session, "Error: not your turn");
      return false;
    }

    // Check if game is in checkmate
    if (chessGame.isInCheckmate(ChessGame.TeamColor.WHITE) ||
            chessGame.isInCheckmate(ChessGame.TeamColor.BLACK)) {
      sendError(session, "No moves to be made.");
      return false;
    }

    return true;
  }

  private boolean isPlayersTurn(ChessGame game, boolean isWhite, boolean isBlack) {
    return !((game.getTeamTurn() != ChessGame.TeamColor.WHITE && isWhite) ||
            (game.getTeamTurn() != ChessGame.TeamColor.BLACK && isBlack));
  }

  private void sendGameUpdates(int gameId, GameData game, AuthData auth, MakeMove moveCommand) throws IOException {
    Map<Session, String> gameSessions = GAME_CONNECTIONS.get(gameId);
    if (gameSessions == null) return;

    // Prepare update messages
    LoadGame loadGame = new LoadGame(game.game());
    String loadGameJson = gson.toJson(loadGame);

    String moveNotification = String.format("%s moved from %s to %s",
            auth.username(),
            moveCommand.getMove().getStartPosition(),
            moveCommand.getMove().getEndPosition());
    String notificationJson = gson.toJson(new Notification(moveNotification));

    // Send updates to all connected clients
    boolean isWhiteMove = auth.username().equals(game.whiteUsername());
    for (Map.Entry<Session, String> entry : gameSessions.entrySet()) {
      if (!entry.getKey().isOpen()) continue;

      sendUpdatesToClient(entry.getKey(), entry.getValue(), game,
              loadGameJson, notificationJson, isWhiteMove);
    }
  }

  private void sendUpdatesToClient(Session clientSession, String username,
                                   GameData game, String loadGameJson,
                                   String notificationJson, boolean isWhiteMove) throws IOException {
    // Always send game state update
    clientSession.getRemote().sendString(loadGameJson);

    boolean isObserver = !username.equals(game.whiteUsername()) &&
            !username.equals(game.blackUsername());
    boolean shouldNotify = isWhiteMove ?
            (username.equals(game.blackUsername()) || isObserver) :
            (username.equals(game.whiteUsername()) || isObserver);

    if (shouldNotify) {
      clientSession.getRemote().sendString(notificationJson);
    }
  }

  private void handlePostMoveGameState(int gameId, ChessGame chessGame) {
    ChessGame.TeamColor currentTeam = chessGame.getTeamTurn();

    if (chessGame.isInCheckmate(currentTeam)) {
      ChessGame.TeamColor winner = (currentTeam == ChessGame.TeamColor.WHITE) ?
              ChessGame.TeamColor.BLACK :
              ChessGame.TeamColor.WHITE;
      broadcastNotification(gameId, String.format("Checkmate! %s wins!", winner), null);
    } else if (chessGame.isInCheck(currentTeam)) {
      broadcastNotification(gameId, String.format("%s is in check!", currentTeam), null);
    }
  }

  private void SendGame(ChessGame.TeamColor color, String username, GameData game, Session clientSession, String notificationJson) throws IOException {
    // If white moved, send to black player and observers
    if (username.equals(game.blackUsername()) || (!username.equals(game.whiteUsername()) && !username.equals(game.blackUsername()))) {
      clientSession.getRemote().sendString(notificationJson);
    }
  }

  private void handleResign(Session session, UserGameCommand command, AuthData auth, GameData game) {
    if (!auth.username().equals(game.whiteUsername()) && !auth.username().equals(game.blackUsername())) {
      sendError(session, "Error: only players can resign");
      return;
    }
    if (game.game().getTeamTurn() == ChessGame.TeamColor.RESIGNED) {
      sendError(session, "SUCK IT HE ALREADY RESIGNED");
      return;


    }

    game.game().setTeamTurn(ChessGame.TeamColor.RESIGNED);
    System.out.println("Team turn: " + game.game().getTeamTurn().toString());
    broadcastNotification(command.getGameID(), String.format("%s resigned from the game", auth.username()), null);


    try {
      Server.gameDAO.updateGame(game);
    } catch (DataAccessException e) {
      throw new RuntimeException(e);
    }
  }


  private void handleLeave(Session session, UserGameCommand command, AuthData auth, GameData game) {
    Map<Session, String> gameSessions=GAME_CONNECTIONS.get(command.getGameID());
    if (gameSessions != null) {
      gameSessions.remove(session);
      broadcastNotification(command.getGameID(), String.format("%s left the game", auth.username()), session);
    }

    try {
      if (auth.username().equals(game.whiteUsername())) {
        GameData updatedGame=new GameData(game.gameID(), null, game.blackUsername(), game.gameName(), game.game());
        Server.gameDAO.updateGame(updatedGame);
      } else if (auth.username().equals(game.blackUsername())) {
        GameData updatedGame=new GameData(game.gameID(), game.whiteUsername(), null, game.gameName(), game.game());
        Server.gameDAO.updateGame(updatedGame);

      }
    } catch (DataAccessException e) {
      throw new RuntimeException(e);
    }

  }


  private void sendError(Session session, String message) {
    try {
      System.out.println("session for sending error: " + session.hashCode());
      System.out.println("session get remote: " + session.getRemote().hashCode());
      Error error=new Error(message);

      session.getRemote().sendString(gson.toJson(error));
    } catch (Exception e) {
      System.err.println("❌ [WS-ERROR] Failed to send error: " + e.getMessage());
    }
  }


  private void broadcastNotification(int gameId, String message, Session exclude) {
    try {
      Notification notification=new Notification(message);
      String jsonNotification=gson.toJson(notification);
      Map<Session, String> sessions=GAME_CONNECTIONS.get(gameId);
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
  public void onWebSocketClose(Session session, int statusCode, String reason) {
    System.out.println("\n⚡ [DEBUG] onWebSocketClose triggered");
    //Session session = getSession();
    System.out.println("\n🔴 [WS-CLOSE] Connection closing for session " + session.hashCode());
    System.out.println("🔴 [WS-CLOSE] Status: " + statusCode + ", Reason: " + reason);

    if (session != null) {
      // Clean up all game connections for this session
      GAME_CONNECTIONS.values().forEach(sessions -> {
        String username=sessions.remove(session);
        if (username != null) {
          broadcast(sessions, gson.toJson(new Notification(username + " disconnected")));
        }
      });
    }
  }

  @OnWebSocketError
  public void onWebSocketError(Throwable cause) {
    System.out.println("\n⚡ [DEBUG] onWebSocketError triggered");
    System.err.println("\n❌ [WS-ERROR] WebSocket error occurred:");
    cause.printStackTrace();
  }
}