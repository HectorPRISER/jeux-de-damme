package dames;

import java.util.List;
import java.util.Random;

/** IA simple : minimax avec élagage alpha-bêta et table de transposition, sur une évaluation matérielle. */
public final class Bot {
    private static final double LOSS = -1000;
    private static final double PAWN = 1;
    private static final double KING = 3;

    // Ce que la table retient d'une position : sa valeur exacte, ou seulement une borne (voir negamax).
    private static final int EXACT = 0;
    private static final int LOWER = 1;
    private static final int UPPER = 2;

    // Une clé aléatoire par (case, type de pièce) et une pour « les noirs jouent » : le XOR des clés d'une position l'identifie.
    private static final long[] ZOBRIST = new long[Board.SIZE * Board.SIZE * 4 + 1];

    static {
        Random random = new Random(42);
        for (int i = 0; i < ZOBRIST.length; i++) ZOBRIST[i] = random.nextLong();
    }

    private final int depth;
    // Table de transposition : une position atteinte par plusieurs ordres de coups n'est calculée qu'une fois.
    private final long[] keys;
    private final double[] values;
    private final short[] info; // profondeur restante (bits 2 et plus) + type de valeur (bits 0-1)
    private final int mask;

    public Bot(int depth) {
        this.depth = depth;
        int size = 1 << Math.min(20, depth + 8);
        keys = new long[size];
        values = new double[size];
        info = new short[size];
        mask = size - 1;
    }

    /** Choisit le meilleur coup pour {@code color} dans la position donnée. */
    public Move chooseMove(Board board, Color color) {
        List<Move> moves = MoveGenerator.legalMoves(board, color);
        if (moves.isEmpty()) return null;

        Move best = moves.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Move move : moves) {
            Board.Undo undo = board.apply(move);
            // Un coup qui ne dépasse pas le meilleur score déjà trouvé ne sert à rien : on peut couper tôt.
            double score = -negamax(board, color.opposite(), depth - 1, Double.NEGATIVE_INFINITY, -bestScore);
            board.undo(move, undo);
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
        double alphaStart = alpha;
        long key = hash(board, color);
        int slot = (int) key & mask;
        int tag = depth << 2;
        if (keys[slot] == key && (info[slot] & ~3) == tag) { // même position, même profondeur restante : valeur connue
            int kind = info[slot] & 3;
            if (kind == EXACT) return values[slot];
            if (kind == LOWER) alpha = Math.max(alpha, values[slot]);
            else beta = Math.min(beta, values[slot]);
            if (alpha >= beta) return values[slot];
        }

        List<Move> moves = MoveGenerator.legalMoves(board, color);
        if (moves.isEmpty()) return LOSS;

        double best;
        if (depth == 0) {
            best = evaluate(board, color);
        } else {
            best = Double.NEGATIVE_INFINITY;
            for (Move move : moves) {
                Board.Undo undo = board.apply(move);
                double score = -negamax(board, color.opposite(), depth - 1, -beta, -alpha);
                board.undo(move, undo);
                best = Math.max(best, score);
                alpha = Math.max(alpha, score);
                if (alpha >= beta) break;
            }
        }

        // Une valeur trouvée avec une fenêtre (alpha, beta) n'est exacte que si elle est strictement à l'intérieur.
        keys[slot] = key;
        values[slot] = best;
        info[slot] = (short) (tag | (best <= alphaStart ? UPPER : best >= beta ? LOWER : EXACT));
        return best;
    }

    private static long hash(Board board, Color color) {
        long h = color == Color.BLACK ? ZOBRIST[ZOBRIST.length - 1] : 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.get(r, c);
                if (p == null) continue;
                h ^= ZOBRIST[(r * Board.SIZE + c) * 4 + (p.color() == Color.WHITE ? 0 : 2) + (p.king() ? 1 : 0)];
            }
        }
        return h;
    }

    private double evaluate(Board board, Color color) {
        double score = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.get(r, c);
                if (p == null) continue;
                double value = p.king() ? KING : PAWN;
                score += p.color() == color ? value : -value;
            }
        }
        return score;
    }
}
