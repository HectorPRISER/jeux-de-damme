import com.sun.management.ThreadMXBean;
import dames.Board;
import dames.Bot;
import dames.Color;
import dames.MoveGenerator;
import java.lang.management.ManagementFactory;

/**
 * Mesure la mémoire : octets alloués sur le tas, de façon exacte (ThreadMXBean).
 * Complète hyperfine, qui mesure le temps mais pas les allocations.
 * Attention : ne compte que le thread courant (à adapter si la recherche devient multi-threads).
 *
 * Usage : java Bench [profondeur=6]
 */
public class Bench {
    private static final ThreadMXBean BEAN = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final int RUNS = 5;
    private static int sink; // consomme les résultats pour que la JVM ne supprime pas le travail mesuré

    private record Result(double millis, long bytes) {}

    public static void main(String[] args) {
        int depth = args.length > 0 ? Integer.parseInt(args[0]) : 6;
        Board board = Board.initial();
        Bot bot = new Bot(depth);

        int calls = 200_000;
        Result gen = measure(() -> {
            for (int i = 0; i < calls; i++) sink += MoveGenerator.legalMoves(board, Color.WHITE).size();
        });
        System.out.printf("legalMoves    : %.2f µs/appel, %d o/appel%n", gen.millis() * 1000 / calls, gen.bytes() / calls);

        Result search = measure(() -> sink += bot.chooseMove(board, Color.WHITE).toString().length());
        System.out.printf("chooseMove d=%d: %.0f ms, %.1f Mo alloués%n", depth, search.millis(), search.bytes() / 1_048_576.0);
    }

    /** Un passage de chauffe, puis {@code RUNS} passages : meilleur temps, et octets alloués par passage. */
    private static Result measure(Runnable task) {
        task.run();
        double bestMillis = Double.MAX_VALUE;
        long bytes = 0;
        for (int i = 0; i < RUNS; i++) {
            long allocBefore = BEAN.getCurrentThreadAllocatedBytes();
            long start = System.nanoTime();
            task.run();
            bestMillis = Math.min(bestMillis, (System.nanoTime() - start) / 1e6);
            bytes = BEAN.getCurrentThreadAllocatedBytes() - allocBefore;
        }
        return new Result(bestMillis, bytes);
    }
}
