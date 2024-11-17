package ui;

import client.ServerFacade;
import static ui.EscapeSequences.*;
import java.util.*;

public class PostLoginREPL {
  private final ServerFacade facade;
  private final List<model.GameData> gameCache;
  private boolean active;

  public PostLoginREPL(ServerFacade facade) {
    this.facade = facade;
    this.gameCache = new ArrayList<>();
    this.active = true;
  }

  public void run() {
    var scanner = new Scanner(System.in);
    while (active) {
      System.out.print("\n[IN] >> ");
      String[] args = scanner.nextLine().toLowerCase().split(" ");
      processCommand(args);
    }
  }

  private void processCommand(String[] args) {
    switch (args[0]) {
      case "help" -> displayHelp();
      case "create" -> createGame(args);
      case "list" -> listGames();
      case "join" -> joinGame(args);
      case "observe" -> observeGame(args);
      case "logout" -> logout();
      case "quit" -> active = false;
      default -> System.out.println("Unknown command - try 'help'");
    }
  }

  private void createGame(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage: create <name>");
      return;
    }
    int id = facade.createGame(args[1]);
    System.out.println(id >= 0 ? "Game created" : "Creation failed");
  }

  private void listGames() {
    gameCache.clear();
    gameCache.addAll(facade.listGames());
    if (gameCache.isEmpty()) {
      System.out.println("No games available");
      return;
    }
    for (int i = 0; i < gameCache.size(); i++) {
      var game = gameCache.get(i);
      System.out.printf("%d: %s (W: %s, B: %s)%n",
              i + 1, game.gameName(),
              game.whiteUsername() != null ? game.whiteUsername() : "OPEN",
              game.blackUsername() != null ? game.blackUsername() : "OPEN");
    }
  }

  private void joinGame(String[] args) {
    if (args.length != 3 || !args[2].matches("(?i)(WHITE|BLACK)")) {
      System.out.println("Usage: join <id> <WHITE|BLACK>");
      return;
    }
    try {
      int gameId = getGameId(args[1]);
      if (facade.joinGame(gameId, args[2].toUpperCase())) {
        System.out.println("Joined game");
        drawBoard();
      } else {
        System.out.println("Failed to join");
      }
    } catch (Exception e) {
      System.out.println("Invalid game selection");
    }
  }

  private void observeGame(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: observe <id>");
      return;
    }
    try {
      int gameId = getGameId(args[1]);
      if (facade.joinGame(gameId, null)) {
        System.out.println("Observing game");
        drawBoard();
      } else {
        System.out.println("Failed to observe");
      }
    } catch (Exception e) {
      System.out.println("Invalid game selection");
    }
  }

  private int getGameId(String input) {
    int index = Integer.parseInt(input) - 1;
    if (index < 0 || index >= gameCache.size()) {
      throw new IllegalArgumentException("Invalid game index");
    }
    return gameCache.get(index).gameID();
  }

  private void logout() {
    if (facade.logout()) {
      System.out.println("Logged out");
      active = false;
    } else {
      System.out.println("Logout failed");
    }
  }

  private void displayHelp() {
    System.out.println("""
            Available commands:
            create <name>
            list
            join <id> <WHITE|BLACK>
            observe <id>
            help
            logout
            quit""");
  }

  private void drawBoard() {
    var board = new chess.ChessBoard();
    board.resetBoard();
    System.out.println("\nBlack view:");
    ChessBoardMaker.drawBoard(board, true);
    System.out.println("\nWhite view:");
    ChessBoardMaker.drawBoard(board, false);
  }
}