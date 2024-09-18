package chess.Rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class KnightMoveRules {

  /**
   * Returns all valid moves for a knight on the given board from the given position.
   *
   * @param board The current chessboard
   * @param position The position of the knight
   * @return A collection of valid moves for the knight
   */
  public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition position) {
    Collection<ChessMove> validMoves = new ArrayList<>();
    ChessPiece knight = board.getPiece(position);

    int[][] knightMoves = {
            {2, 1}, {2, -1},
            {-2, 1}, {-2, -1},
            {1, 2}, {1, -2},
            {-1, 2}, {-1, -2}
    };

    for (int[] move : knightMoves) {
      int newRow = position.getRow() + move[0];
      int newCol = position.getColumn() + move[1];

      if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
        ChessPosition newPosition = new ChessPosition(newRow, newCol);
        ChessPiece occupyingPiece = board.getPiece(newPosition);

        if (occupyingPiece == null || occupyingPiece.getTeamColor() != knight.getTeamColor()) {
          validMoves.add(new ChessMove(position, newPosition, null));
        }
      }
    }

    return validMoves;
  }
}