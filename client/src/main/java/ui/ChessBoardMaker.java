package ui;

import chess.*;
import static ui.EscapeSequences.*;

public class ChessBoardMaker {
  public static void drawBoard(ChessBoard board, boolean blackPerspective) {
    drawHeaderFooter(blackPerspective);
    drawBoardBody(board, blackPerspective);
    drawHeaderFooter(blackPerspective);
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

  private static void drawBoardBody(ChessBoard board, boolean blackPerspective) {
    for (int row = 8; row >= 1; row--) {
      int displayRow = row;
      System.out.print(SET_TEXT_COLOR_YELLOW + SET_TEXT_BOLD + " " + displayRow + "  " + RESET_TEXT_BOLD_FAINT);

      for (int col = 1; col <= 8; col++) {
        int displayCol = blackPerspective ? 9 - col : col;
        boolean isLightSquare = (displayRow + displayCol) % 2 == 1;

        String bgColor = isLightSquare ? SET_BG_COLOR_WHITE : SET_BG_COLOR_DARK_GREY;
        System.out.print(bgColor);

        ChessPosition position = new ChessPosition(row, blackPerspective ? 9 - col : col);
        ChessPiece piece = board.getPiece(position);

        String textColor = piece != null && piece.getTeamColor() == ChessGame.TeamColor.WHITE ?
                SET_TEXT_COLOR_BLUE : SET_TEXT_COLOR_RED;
        System.out.print(textColor);

        System.out.print(getPieceString(piece));
        System.out.print(RESET_BG_COLOR);
      }
      System.out.println(SET_TEXT_COLOR_YELLOW + SET_TEXT_BOLD + "  " + displayRow + RESET_TEXT_BOLD_FAINT);
    }
  }

  private static String getPieceString(ChessPiece piece) {
    if (piece == null) return EMPTY;

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