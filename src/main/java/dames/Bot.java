package dames;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** IA simple : minimax (sans élagage) sur une évaluation matérielle, coups de départ explorés en parallèle. */
public final class Bot {
    private static final double LOSS = -1000;
    private static final double PAWN = 1;
    private static final double KING = 3;
    private static final int THREADS = Runtime.getRuntime().availableProcessors();

    private final int depth;

    public Bot(int depth) {
        this.depth = depth;
    }

    private record MoveScore(Move move, double score) {}

    /**
     * Choisit le meilleur coup pour {@code color} : coups de départ explorés en parallèle
     * (pool fixe = nb de cœurs). Chaque thread reçoit sa propre copie du plateau — il n'y a
     * pas de partage mutable entre threads ; à l'intérieur d'un thread, {@link #negamax}
     * joue/annule en place sur cette copie privée.
     */
    public Move chooseMove(Board board, Color color) {
        List<Move> moves = MoveGenerator.legalMoves(board, color);
        if (moves.isEmpty()) return null;

        List<Callable<MoveScore>> tasks = new ArrayList<>();
        for (Move move : moves) {
            tasks.add(() -> {
                Board next = board.copy();
                next.apply(move);
                return new MoveScore(move, -negamax(next, color.opposite(), depth - 1));
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(THREADS, moves.size()));
        try {
            List<Future<MoveScore>> futures = pool.invokeAll(tasks);
            Move best = moves.get(0);
            double bestScore = Double.NEGATIVE_INFINITY;
            for (Future<MoveScore> future : futures) {
                MoveScore result = future.get();
                if (result.score() > bestScore) {
                    bestScore = result.score();
                    best = result.move();
                }
            }
            return best;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pool.shutdown();
        }
    }

    private double negamax(Board board, Color color, int depth) {
        List<Move> moves = MoveGenerator.legalMoves(board, color);
        if (moves.isEmpty()) return LOSS;
        if (depth == 0) return evaluate(board, color);

        double best = Double.NEGATIVE_INFINITY;
        for (Move move : moves) {
            Board.Undo undo = board.apply(move);
            double score = -negamax(board, color.opposite(), depth - 1);
            board.undo(move, undo);
            best = Math.max(best, score);
        }
        return best;
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
