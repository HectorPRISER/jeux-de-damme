package dames;

import java.util.List;

public class Game {
    private final Board board;
    private Color turn;

    public Game() {
        this(Board.initial(), Color.WHITE);
    }

    public Game(Board board, Color turn) {
        this.board = board;
        this.turn = turn;
    }

    public Board board() {
        return board;
    }

    public Color turn() {
        return turn;
    }

    public List<Move> legalMoves() {
        return MoveGenerator.legalMoves(board, turn);
    }

    public boolean isOver() {
        return legalMoves().isEmpty();
    }

    /** Le joueur qui ne peut plus jouer perd. */
    public Color winner() {
        return isOver() ? turn.opposite() : null;
    }

    public void play(Move move) {
        if (!legalMoves().contains(move)) {
            throw new IllegalArgumentException("Coup illégal : " + move);
        }
        board.apply(move);
        turn = turn.opposite();
    }
}
