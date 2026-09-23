package dames;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/** Métriques (temps, débit, octets alloués) sur {@link MoveGenerator} et {@link Bot} — baseline avant optimisation. */
public final class Bench {
    private static final ThreadMXBean BEAN = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long THREAD_ID = Thread.currentThread().threadId();

    private Bench() {}

    public static void main(String[] args) {
        System.out.println("=== MoveGenerator.legalMoves (position initiale) ===");
        benchMoveGenerator(Board.initial(), Color.WHITE, 200_000);

        System.out.println();
        System.out.println("=== Bot.chooseMove (minimax brut, position initiale) ===");
        for (int depth : new int[] {4, 5, 6}) {
            benchBot(depth);
        }
    }

    private static void benchMoveGenerator(Board board, Color color, int iterations) {
        for (int i = 0; i < 10_000; i++) {
            MoveGenerator.legalMoves(board, color);
        }

        long allocBefore = BEAN.getThreadAllocatedBytes(THREAD_ID);
        long start = System.nanoTime();
        int totalMoves = 0;
        for (int i = 0; i < iterations; i++) {
            totalMoves += MoveGenerator.legalMoves(board, color).size();
        }
        long elapsedNanos = System.nanoTime() - start;
        long allocated = BEAN.getThreadAllocatedBytes(THREAD_ID) - allocBefore;

        double perCallUs = elapsedNanos / 1000.0 / iterations;
        double perCallBytes = (double) allocated / iterations;
        double perSecond = iterations / (elapsedNanos / 1e9);

        System.out.printf(
                "  %,d appels : %.2f µs/appel, %,.0f appels/s, %.0f o/appel (%d coups générés au dernier appel)%n",
                iterations, perCallUs, perSecond, perCallBytes, totalMoves / iterations);
    }

    private static void benchBot(int depth) {
        Bot bot = new Bot(depth);
        Board board = Board.initial();

        long allocBefore = BEAN.getThreadAllocatedBytes(THREAD_ID);
        long start = System.nanoTime();
        Move move = bot.chooseMove(board, Color.WHITE);
        long elapsedNanos = System.nanoTime() - start;
        long allocated = BEAN.getThreadAllocatedBytes(THREAD_ID) - allocBefore;

        long nodes = bot.nodesExplored();
        double nodesPerSecond = nodes / (elapsedNanos / 1e9);
        double bytesPerNode = (double) allocated / nodes;

        System.out.printf(
                "  profondeur %d : coup=%s, %,d noeuds, %,.0f ms, %,.0f noeuds/s, %.1f Mo allouées (%.0f o/noeud)%n",
                depth, move, nodes, elapsedNanos / 1e6, nodesPerSecond, allocated / 1_048_576.0, bytesPerNode);
    }
}
