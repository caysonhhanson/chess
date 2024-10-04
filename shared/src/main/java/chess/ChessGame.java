package chess;

import java.util.Collection;
import java.util.ArrayList;

public class ChessGame {

    private TeamColor teamTurn;
    private ChessBoard board;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
    }

    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    public enum TeamColor {
        WHITE,
        BLACK
    }

    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return new ArrayList<>(); // Return an empty list instead of null
        }

        Collection<ChessMove> possibleMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new ArrayList<>();

        for (ChessMove move : possibleMoves) {
            ChessBoard tempBoard = new ChessBoard();
            for (int row = 1; row <= 8; row++) {
                for (int col = 1; col <= 8; col++) {
                    ChessPosition position = new ChessPosition(row, col);
                    ChessPiece boardPiece = board.getPiece(position);
                    if (boardPiece != null) {
                        tempBoard.addPiece(position, new ChessPiece(boardPiece.getTeamColor(), boardPiece.getPieceType()));
                    }
                }
            }

            tempBoard.addPiece(move.getEndPosition(), piece);
            tempBoard.addPiece(move.getStartPosition(), null);

            ChessGame tempGame = new ChessGame();
            tempGame.setBoard(tempBoard);

            if (!tempGame.isInCheck(piece.getTeamColor())) {
                validMoves.add(move);
            }
        }

        return validMoves;
    }

    public void makeMove(ChessMove move) throws InvalidMoveException {
        Collection<ChessMove> validMoves = validMoves(move.getStartPosition());
        if (validMoves == null || !validMoves.contains(move)) {
            throw new InvalidMoveException("Invalid move");
        }

        ChessPiece piece = board.getPiece(move.getStartPosition());
        board.addPiece(move.getEndPosition(), piece);
        board.addPiece(move.getStartPosition(), null);

        if (move.getPromotionPiece() != null) {
            board.addPiece(move.getEndPosition(), new ChessPiece(piece.getTeamColor(), move.getPromotionPiece()));
        }

        setTeamTurn(teamTurn == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE);
    }

    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = null;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getPieceType() == ChessPiece.PieceType.KING && piece.getTeamColor() == teamColor) {
                    kingPosition = position;
                    break;
                }
            }
            if (kingPosition != null) break;
        }

        if (kingPosition == null) {
            return false;
        }

        TeamColor opposingColor = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == opposingColor) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, position);
                    for (ChessMove move : moves) {
                        if (move.getEndPosition().equals(kingPosition)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
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

    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    public ChessBoard getBoard() {
        return board;
    }
}
