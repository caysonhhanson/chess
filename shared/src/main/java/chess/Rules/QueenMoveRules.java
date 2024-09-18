package chess.Rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class QueenMoveRules {

  public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
    Collection<ChessMove> validMoves = new ArrayList<>();
    ChessPiece queen = board.getPiece(position);

    int[][] directions = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    for (int[] direction : directions) {
      int directionRow = direction[0];
      int directionCol = direction[1];
      int currentRow = position.getRow();
      int currentCol = position.getColumn();

      while (true) {
        currentRow += directionRow;
        currentCol += directionCol;

        if (currentRow < 1 || currentRow > 8 || currentCol < 1 || currentCol > 8) {
          break;
        }

        ChessPosition newPosition = new ChessPosition(currentRow, currentCol);

        ChessPiece occupyingPiece = board.getPiece(newPosition);

        if (occupyingPiece == null) {
          validMoves.add(new ChessMove(position, newPosition, null));
        } else if (occupyingPiece.getTeamColor() != queen.getTeamColor()) {
          validMoves.add(new ChessMove(position, newPosition, null));
          break;
        } else {
          break;
        }
      }
    }
    return validMoves;
  }
}