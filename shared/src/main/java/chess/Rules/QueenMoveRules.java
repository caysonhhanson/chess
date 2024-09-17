package chess.Rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class QueenMoveRules {

  /**
   * Returns all valid moves for a rook on the given board from the given position.
   *
   * @param board The current chessboard
   * @param position The position of the rook
   * @return A collection of valid moves for the rook
   */
  public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
    Collection<ChessMove> validMoves = new ArrayList<>();
    ChessPiece queen = board.getPiece(position);

    if (queen == null || queen.getPieceType() != ChessPiece.PieceType.QUEEN) {
      return validMoves;
    }

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