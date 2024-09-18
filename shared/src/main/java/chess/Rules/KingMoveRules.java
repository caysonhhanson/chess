package chess.Rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class KingMoveRules {

 public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition myPosition) {
    Collection<ChessMove> moves = new ArrayList<>();
    ChessPiece king = board.getPiece(myPosition);

    int[][] directions = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1},
            {-1, -1},
            {-1, 1},
            {1, -1},
            {1, 1}
    };

    for (int[] direction : directions) {
      int newRow = myPosition.getRow() + direction[0];
      int newCol = myPosition.getColumn() + direction[1];

      if (isInBounds(newRow, newCol)) {
        ChessPosition newPosition = new ChessPosition(newRow, newCol);
        ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

        if (pieceAtNewPosition == null || pieceAtNewPosition.getTeamColor() != king.getTeamColor()) {
          moves.add(new ChessMove(myPosition, newPosition, null));
        }
      }
    }
    return moves;
  }

  private static boolean isInBounds(int row, int col) {
    return row >= 1 && row <= 8 && col >= 1 && col <= 8;
  }
}
