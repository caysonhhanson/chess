package ui;

import chess.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import static ui.EscapeSequences.*;

public class ChessBoardMaker {
  private static final Queue<String> NOTIFICATIONS= new LinkedList<>();
  private static final int MAX_NOTIFICATIONS = 5;

  public static void addNotification(String message) {
    NOTIFICATIONS.offer(message);
    if (NOTIFICATIONS.size() > MAX_NOTIFICATIONS) {
      NOTIFICATIONS.poll();
    }
  }

  public static void drawBoard(ChessBoard board, boolean blackPerspective, Collection<ChessMove> highlights) {
    drawNotifications();
    System.out.println();

    drawHeaderFooter(blackPerspective);
    drawBoardBody(board, blackPerspective, highlights);
    drawHeaderFooter(blackPerspective);
  }

  private static void drawNotifications() {
    System.out.println(SET_TEXT_COLOR_GREEN + "Recent notifications:" + RESET_TEXT_COLOR);
    if (NOTIFICATIONS.isEmpty()) {
      System.out.println("No notifications");
      return;
    }
    for (String notification : NOTIFICATIONS) {
      System.out.println(SET_TEXT_COLOR_BLUE + "→ " + notification + RESET_TEXT_COLOR);
    }
  }

  public static void drawBoard(ChessBoard board, boolean blackPerspective) {
    drawBoard(board, blackPerspective, null);
  }

  private static void drawHeaderFooter(boolean blackPerspective) {
    System.out.print("    ");
    char[] columns = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
    if (blackPerspective) {
      for (int i = columns.length - 1; i >= 0; i--) {
        System.out.print(SET_TEXT_BOLD + SET_TEXT_COLOR_BLUE + columns[i] + "  " + RESET_TEXT_BOLD_FAINT);
      }
    } else {
      for (char column : columns) {
        System.out.print(SET_TEXT_BOLD + SET_TEXT_COLOR_BLUE + column + "  " + RESET_TEXT_BOLD_FAINT);
      }
    }
    System.out.println();
  }

  private static void drawBoardBody(ChessBoard board, boolean blackPerspective, Collection<ChessMove> highlights) {
    int startRow = blackPerspective ? 1 : 8;
    int endRow = blackPerspective ? 8 : 1;
    int increment = blackPerspective ? 1 : -1;

    for (int row = startRow; blackPerspective ? row <= endRow : row >= endRow; row += increment) {
      System.out.print(SET_TEXT_COLOR_YELLOW + SET_TEXT_BOLD + " " + row + "  " + RESET_TEXT_BOLD_FAINT);

      for (int col = 1; col <= 8; col++) {
        int displayCol = blackPerspective ? 9 - col : col;
        boolean isLightSquare = ((row + displayCol) % 2 == 1);
        ChessPosition position = blackPerspective ?
                new ChessPosition(9 - row, 9 - col) :
                new ChessPosition(row, col);

        boolean isHighlighted = false;
        if (highlights != null) {
          for (ChessMove move : highlights) {
            if (move.getStartPosition().equals(position) ||
                    move.getEndPosition().equals(position)) {
              isHighlighted = true;
              break;
            }
          }
        }

        String bgColor = isHighlighted ? SET_BG_COLOR_GREEN :
                isLightSquare ? SET_BG_COLOR_WHITE :
                        SET_BG_COLOR_DARK_GREY;
        System.out.print(bgColor);

        ChessPiece piece = board.getPiece(position);
        if (piece != null) {
          boolean isPieceWhite = piece.getTeamColor() == ChessGame.TeamColor.WHITE;
          String textColor = blackPerspective ?
                  (isPieceWhite ? SET_TEXT_COLOR_RED : SET_TEXT_COLOR_BLUE) :
                  (isPieceWhite ? SET_TEXT_COLOR_BLUE : SET_TEXT_COLOR_RED);
          System.out.print(textColor);
        }

        System.out.print(getPieceString(piece));
        System.out.print(RESET_BG_COLOR);
      }
      System.out.println(SET_TEXT_COLOR_YELLOW + SET_TEXT_BOLD + "  " + row + RESET_TEXT_BOLD_FAINT);
    }
  }

  private static String getPieceString(ChessPiece piece) {
    if (piece == null) {
      return EMPTY;
    }
    return switch (piece.getPieceType()) {
      case KING -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_KING : BLACK_KING;
      case QUEEN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_QUEEN : BLACK_QUEEN;
      case BISHOP -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_BISHOP : BLACK_BISHOP;
      case KNIGHT -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_KNIGHT : BLACK_KNIGHT;
      case ROOK -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_ROOK : BLACK_ROOK;
      case PAWN -> piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_PAWN : BLACK_PAWN;
    };
  }
}