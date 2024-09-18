package chess.Rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class PawnMoveRules {

  /**
   * Returns all valid pawn moves from the given position on the given board.
   * Pawns move differently depending on the team (White moves up, Black moves down).
   *
   * @param board        the chess board
   * @param myPosition   the current position of the pawn
   * @return a collection of valid moves for the pawn
   */
  public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition myPosition) {
    Collection<ChessMove> moves = new ArrayList<>();
    ChessPiece pawn = board.getPiece(myPosition);

    int direction = (pawn.getTeamColor() == ChessGame.TeamColor.WHITE) ? 1 : -1;
    int startRow = (pawn.getTeamColor() == ChessGame.TeamColor.WHITE) ? 2 : 7;
    int promotionRow = (pawn.getTeamColor() == ChessGame.TeamColor.WHITE) ? 8 : 1;

    addMoveIfValid(board, moves, pawn, myPosition, myPosition.getRow() + direction, myPosition.getColumn(), false, promotionRow);

    if (myPosition.getRow() == startRow) {
      ChessPosition oneSquareAhead = new ChessPosition(myPosition.getRow() + direction, myPosition.getColumn());
      if (board.getPiece(oneSquareAhead) == null) {
        addMoveIfValid(board, moves, pawn, myPosition, myPosition.getRow() + 2 * direction, myPosition.getColumn(), false, promotionRow);
      }
    }
    addMoveIfValid(board, moves, pawn, myPosition, myPosition.getRow() + direction, myPosition.getColumn() - 1, true, promotionRow);
    addMoveIfValid(board, moves, pawn, myPosition, myPosition.getRow() + direction, myPosition.getColumn() + 1, true, promotionRow);

    return moves;
  }

  private static void addMoveIfValid(ChessBoard board, Collection<ChessMove> moves, ChessPiece pawn, ChessPosition oldPosition, int toRow, int toColumn, boolean isCapture, int promotionRow) {
    if (isInBounds(toRow, toColumn)) {
      ChessPosition newPosition = new ChessPosition(toRow, toColumn);
      ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

      if (isCapture && pieceAtNewPosition != null && pieceAtNewPosition.getTeamColor() != pawn.getTeamColor()) {
        addPromotionMove(moves, oldPosition, newPosition, toRow, promotionRow);
      }
      else if (!isCapture && pieceAtNewPosition == null) {
        addPromotionMove(moves, oldPosition, newPosition, toRow, promotionRow);
      }
    }
  }

  private static void addPromotionMove(Collection<ChessMove> moves, ChessPosition from, ChessPosition newPosition, int toRow, int promotionRow) {
    if (toRow == promotionRow) {
      moves.add(new ChessMove(from, newPosition, ChessPiece.PieceType.QUEEN));
      moves.add(new ChessMove(from, newPosition, ChessPiece.PieceType.ROOK));
      moves.add(new ChessMove(from, newPosition, ChessPiece.PieceType.KNIGHT));
      moves.add(new ChessMove(from, newPosition, ChessPiece.PieceType.BISHOP));
    } else {
      moves.add(new ChessMove(from, newPosition, null));
    }
  }

  private static boolean isInBounds(int row, int col) {
    return row >= 1 && row <= 8 && col >= 1 && col <= 8;
  }
}