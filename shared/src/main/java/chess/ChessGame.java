package chess;

import java.util.Collection;
import java.util.HashSet;


/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private TeamColor TeamTurn;
    private ChessBoard board;

    public ChessGame() {

    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return TeamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.TeamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece currPiece = board.getPiece(startPosition);
        if (currPiece == null) {
            return null;
        }
        HashSet<ChessMove> possibleMoves = (HashSet<ChessMove>) board.getPiece(startPosition).pieceMoves(board, startPosition);
        HashSet<ChessMove> validMoves = HashSet.newHashSet(possibleMoves.size());
        for (ChessMove move : possibleMoves) {
            ChessPiece possiblePiece = board.getPiece(move.getEndPosition());
            board.addPiece(startPosition, null);
            board.addPiece(move.getEndPosition(), currPiece);
            if (!isInCheck(currPiece.getTeamColor())) {
                validMoves.add(move);
            }
            board.addPiece(move.getEndPosition(), possiblePiece);
            board.addPiece(startPosition, currPiece);

        }
        return validMoves;
    }


    /**
     * Makes a move in a chess game
     *
     * @param move chess move to preform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPos = null;
        for (int y = 1; y <= 8 && kingPos == null; y++) {
            for (int x = 1; x <= 8  && kingPos == null; x++) {
                ChessPiece currPiece = board.getPiece(new ChessPosition(y, x));
                if (currPiece == null) {
                    continue;
                }
                if (currPiece.getTeamColor() == teamColor && currPiece.getPieceType() == ChessPiece.PieceType.KING) {
                    kingPos = new ChessPosition(y, x);
                }
            }
        }
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece currPiece = board.getPiece(new ChessPosition(y, x));
                if (currPiece == null || currPiece.getTeamColor() == teamColor) {
                    continue;
                }
                for (ChessMove enemyMove : currPiece.pieceMoves(board, new ChessPosition(y, x))) {
                    if (enemyMove.getEndPosition().equals(kingPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (isInCheck(teamColor) && isInStalemate(teamColor)){
            return true;
        } else {return false;}
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(position);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard(){
        return board;
    }
}
