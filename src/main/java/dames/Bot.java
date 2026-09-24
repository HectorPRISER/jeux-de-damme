package dames;

import java.util.List;

/** IA simple : minimax avec élagage alpha-bêta sur une évaluation matérielle. */
public final class Bot {
    private static final double LOSS = -1000;
    private static final double PAWN = 1;
    private static final double KING = 3;

    private final int depth;

    public Bot(int depth) {
        this.depth = depth;
    }

    /** Choisit le meilleur coup pour {@code color} dans la position donnée. */
    public Move chooseMove(Board board, Color color) {
        List<Move> moves = MoveGenerator.legalMoves(board, color);
        if (moves.isEmpty()) return null;

        Move best = moves.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Move move : moves) {
            Board next = board.copy();
            next.apply(move);
            // Un coup qui ne dépasse pas le meilleur score déjà trouvé ne sert à rien : on peut couper tôt.
            double score = -negamax(next, color.opposite(), depth - 1, Double.NEGATIVE_INFINITY, -bestScore);
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return best;
    }

    /**
     * Score de la position pour {@code color}. {@code alpha} est le score que {@code color} est déjà sûr d'obtenir,
     * {@code beta} celui au-delà duquel l'adversaire évitera cette position : dès que {@code alpha >= beta}, les
     * coups restants ne peuvent plus changer le résultat, on arrête.
     */
    private double negamax(Board board, Color color, int depth, double alpha, double beta) {
        List<Move> moves = MoveGenerator.legalMoves(board, color);
        if (moves.isEmpty()) return LOSS;
        if (depth == 0) return evaluate(board, color);

        double best = Double.NEGATIVE_INFINITY;
        for (Move move : moves) {
            Board next = board.copy();
            next.apply(move);
            double score = -negamax(next, color.opposite(), depth - 1, -beta, -alpha);
            best = Math.max(best, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta) break;
        }
        return best;
    }

    private double evaluate(Board board, Color color) {
        double score = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.get(new Position(r, c));
                if (p == null) continue;
                double value = p.king() ? KING : PAWN;
                score += p.color() == color ? value : -value;
            }
        }
        return score;
    }
}
