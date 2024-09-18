package chess.Rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class KnightMoveRules {

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