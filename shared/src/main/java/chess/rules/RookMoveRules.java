package chess.rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class RookMoveRules {

  public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
    Collection<ChessMove> validMoves = new ArrayList<>();
    ChessPiece rook = board.getPiece(position);

    int[][] directions = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    for (int[] direction : directions) {
      int dRow = direction[0];
      int dCol = direction[1];
      int currentRow = position.getRow();
      int currentCol = position.getColumn();

      while (true) {
        currentRow += dRow;
        currentCol += dCol;

        if (currentRow < 1 || currentRow > 8 || currentCol < 1 || currentCol > 8) {
          break;
        }
        ChessPosition newPosition = new ChessPosition(currentRow, currentCol);
        ChessPiece occupyingPiece = board.getPiece(newPosition);
        if (occupyingPiece == null) {
          validMoves.add(new ChessMove(position, newPosition, null));
        } else if (occupyingPiece.getTeamColor() != rook.getTeamColor()) {
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
