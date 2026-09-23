package dames;

import java.util.List;
import java.util.Random;

/** Coup légal au hasard, sans recherche — adversaire faible de {@link Bot}. */
public final class RandomBot {
    private final Random random = new Random();

    public Move chooseMove(Board board, Color color) {
        List<Move> moves = MoveGenerator.legalMoves(board, color);
        if (moves.isEmpty()) return null;
        return moves.get(random.nextInt(moves.size()));
    }
}
