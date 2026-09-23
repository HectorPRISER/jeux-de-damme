package dames;

import java.util.List;
import java.util.Random;

/**
 * Stratégie faible de référence : choisit un coup légal au hasard, sans recherche.
 * Sert d'adversaire pour {@link Bot} (minimax) en mode bot contre bot — seul
 * {@link Bot} porte la stratégie gagnante, {@code RandomBot} n'évalue rien.
 */
public final class RandomBot {
    private final Random random = new Random();

    public Move chooseMove(Board board, Color color) {
        List<Move> moves = MoveGenerator.legalMoves(board, color);
        if (moves.isEmpty()) return null;
        return moves.get(random.nextInt(moves.size()));
    }
}
