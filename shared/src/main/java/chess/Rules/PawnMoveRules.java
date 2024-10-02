package chess.Rules;

import chess.*;
import java.util.ArrayList;
import java.util.Collection;

public class PawnMoveRules {

  public static Collection<ChessMove> getMoves(ChessBoard board, ChessPosition myPosition) {
    Collection<ChessMove> validMoves = new ArrayList<>();
    ChessPiece pawn = board.getPiece(myPosition);

    int direction = (pawn.getTeamColor() == ChessGame.TeamColor.WHITE) ? 1 : -1;
    int startRow = (pawn.getTeamColor() == ChessGame.TeamColor.WHITE) ? 2 : 7;
    int promotionRow = (pawn.getTeamColor() == ChessGame.TeamColor.WHITE) ? 8 : 1;

    addMoveIfValid(board, validMoves, pawn, myPosition, myPosition.getRow() + direction, myPosition.getColumn(), false, promotionRow);

    if (myPosition.getRow() == startRow) {
      ChessPosition oneSquareAhead = new ChessPosition(myPosition.getRow() + direction, myPosition.getColumn());
      if (board.getPiece(oneSquareAhead) == null) {
        addMoveIfValid(board, validMoves, pawn, myPosition, myPosition.getRow() + 2 * direction, myPosition.getColumn(), false, promotionRow);
      }
    }
    addMoveIfValid(board, validMoves, pawn, myPosition, myPosition.getRow() + direction, myPosition.getColumn() - 1, true, promotionRow);
    addMoveIfValid(board, validMoves, pawn, myPosition, myPosition.getRow() + direction, myPosition.getColumn() + 1, true, promotionRow);

    return validMoves;
  }

  private static void addMoveIfValid(ChessBoard board, Collection<ChessMove> validMoves, ChessPiece pawn, ChessPosition oldPosition, int toRow, int toColumn, boolean isCapture, int promotionRow) {
    if (isInBounds(toRow, toColumn)) {
      ChessPosition newPosition = new ChessPosition(toRow, toColumn);
      ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

      if (isCapture && pieceAtNewPosition != null && pieceAtNewPosition.getTeamColor() != pawn.getTeamColor()) {
        addPromotionMove(validMoves, oldPosition, newPosition, toRow, promotionRow);
      }
      else if (!isCapture && pieceAtNewPosition == null) {
        addPromotionMove(validMoves, oldPosition, newPosition, toRow, promotionRow);
      }
    }
  }

  private static void addPromotionMove(Collection<ChessMove> validMoves, ChessPosition from, ChessPosition newPosition, int toRow, int promotionRow) {
    if (toRow == promotionRow) {
      validMoves.add(new ChessMove(from, newPosition, ChessPiece.PieceType.QUEEN));
      validMoves.add(new ChessMove(from, newPosition, ChessPiece.PieceType.ROOK));
      validMoves.add(new ChessMove(from, newPosition, ChessPiece.PieceType.KNIGHT));
      validMoves.add(new ChessMove(from, newPosition, ChessPiece.PieceType.BISHOP));
    } else {
      validMoves.add(new ChessMove(from, newPosition, null));
    }
  }

  private static boolean isInBounds(int row, int col) {
    return row >= 1 && row <= 8 && col >= 1 && col <= 8;
  }
}