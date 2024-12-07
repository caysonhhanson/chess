package ui;

import chess.*;
import client.ServerFacade;
import client.WebSocketDecoder;
import websocket.commands.*;

import java.util.Collection;
import java.util.Scanner;

public class GameplayREPL {
  private final ServerFacade server;
  private final WebSocketDecoder webSocket;
  private final int gameId;
  private final String playerColor;
  private ChessGame currentGame;
  private boolean isActive = true;

  public GameplayREPL(ServerFacade server, int gameId, String playerColor) {
    this.server = server;
    this.gameId = gameId;
    this.playerColor = playerColor;

    // Initialize WebSocket with the proper URL format
    this.webSocket = new WebSocketDecoder(
            server.getWebSocketUrl(),
            this::handleNotification,
            this::handleGameUpdate,
            this::handleError
    );
  }

  public void run() {
    try {
      webSocket.connect();

      // Send initial connect command
      UserGameCommand connectCommand = playerColor == null ?
              new JoinObserver(server.getAuthToken(), gameId) :
              new JoinPlayer(server.getAuthToken(), gameId, playerColor);
      webSocket.sendCommand(connectCommand);

      Scanner scanner = new Scanner(System.in);
      while (isActive) {
        System.out.print("\n[GAME] >> ");
        String[] tokens = scanner.nextLine().toLowerCase().split(" ");
        processCommand(tokens);
      }
    } catch (Exception e) {
      System.err.println("Error in gameplay: " + e.getMessage());
    } finally {
      webSocket.disconnect();
    }
  }

  private void processCommand(String[] tokens) {
    if (tokens.length == 0) return;

    switch (tokens[0]) {
      case "help" -> displayHelp();
      case "board" -> redrawBoard();
      case "leave" -> handleLeave();
      case "move" -> handleMove(tokens);
      case "resign" -> handleResign();
      case "highlight" -> handleHighlight(tokens);
      case "quit" -> isActive = false;
      default -> System.out.println("Unknown command. Type 'help' for available commands.");
    }
  }

  private void handleNotification(String message) {
    ChessBoardMaker.addNotification(message);
    if (currentGame != null) {
      redrawBoard();
    }
  }

  private void handleError(String error) {
    System.out.println("\nERROR: " + error);
  }

  private void handleGameUpdate(ChessGame game) {
    this.currentGame = game;
    redrawBoard();
  }

  private void displayHelp() {
    System.out.println("""
                Available commands:
                help - Show this help message
                board - Redraw the chess board
                move <start> <end> - Make a move (e.g., 'move e2 e4')
                highlight <position> - Show legal moves for piece (e.g., 'highlight e2')
                resign - Resign from the game
                leave - Leave the game
                quit - Exit to main menu""");
  }

  private void redrawBoard() {
    if (currentGame != null) {
      boolean blackView = "BLACK".equalsIgnoreCase(playerColor);
      ChessBoardMaker.drawBoard(currentGame.getBoard(), blackView);
    }
  }

  private void handleMove(String[] tokens) {
    if (tokens.length != 3) {
      System.out.println("Usage: move <start> <end> (e.g., 'move e2 e4')");
      return;
    }

    try {
      ChessPosition start = parsePosition(tokens[1]);
      ChessPosition end = parsePosition(tokens[2]);
      ChessMove move = new ChessMove(start, end, null); // Handle promotion separately if needed

      webSocket.sendCommand(new MakeMove(server.getAuthToken(), gameId, move));
    } catch (IllegalArgumentException e) {
      System.out.println("Invalid position format. Use algebraic notation (e.g., e2)");
    }
  }

  private void handleHighlight(String[] tokens) {
    if (tokens.length != 2) {
      System.out.println("Usage: highlight <position> (e.g., 'highlight e2')");
      return;
    }

    try {
      ChessPosition position = parsePosition(tokens[1]);
      if (currentGame != null) {
        Collection<ChessMove> moves = currentGame.validMoves(position);
        // Redraw board with highlights
        ChessBoardMaker.drawBoard(currentGame.getBoard(), "BLACK".equalsIgnoreCase(playerColor), moves);
      }
    } catch (IllegalArgumentException e) {
      System.out.println("Invalid position format. Use algebraic notation (e.g., e2)");
    }
  }

  private void handleResign() {
    System.out.print("Are you sure you want to resign? (yes/no): ");
    Scanner scanner = new Scanner(System.in);
    if (scanner.nextLine().toLowerCase().startsWith("y")) {
      webSocket.sendCommand(new Resign(server.getAuthToken(), gameId));
    }
  }

  private void handleLeave() {
    webSocket.sendCommand(new Leave(server.getAuthToken(), gameId));
    isActive = false;
  }

  private ChessPosition parsePosition(String pos) {
    if (pos.length() != 2) {
      throw new IllegalArgumentException("Invalid position format");
    }

    int col = pos.charAt(0) - 'a' + 1;
    int row = pos.charAt(1) - '0';

    if (col < 1 || col > 8 || row < 1 || row > 8) {
      throw new IllegalArgumentException("Position out of bounds");
    }

    return new ChessPosition(row, col);
  }
}