package chess.Rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class BishopMoveRules {

  /**
   * Returns all valid moves for a bishop on the given board from the given position.
   *
   * @param board The current chessboard
   * @param position The position of the bishop
   * @return A collection of valid moves for the bishop
   */
  public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
    Collection<ChessMove> validMoves = new ArrayList<>();
    ChessPiece bishop = board.getPiece(position);

    if (bishop == null || bishop.getPieceType() != ChessPiece.PieceType.BISHOP) {
      return validMoves;
    }

    int[][] directions = {
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
        } else if (occupyingPiece.getTeamColor() != bishop.getTeamColor()) {
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
