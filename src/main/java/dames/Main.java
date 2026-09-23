package dames;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Jeu de dames internationales (10x10). Blancs (w/W) en bas, noirs (b/B) en haut.");
        System.out.println("Saisie : c3-d4 (déplacement) ou c3-e5-g7 (rafle, ou juste départ-arrivée). 'coups' liste les coups, 'q' quitte.");

        System.out.print("Mode : (1) humain [vs bot en option] (2) bot vs bot > ");
        String mode = in.hasNextLine() ? in.nextLine().trim() : "1";
        if (mode.equals("2")) {
            runBotVsBot(in);
            return;
        }

        Color botColor = chooseOpponent(in);
        Bot bot = botColor == null ? null : new Bot(chooseDepth(in));
        Game game = new Game();

        while (!game.isOver()) {
            System.out.println();
            System.out.print(game.board());
            List<Move> moves = game.legalMoves();

            if (game.turn() == botColor) {
                Move chosen = bot.chooseMove(game.board(), botColor);
                System.out.println((botColor == Color.WHITE ? "Blancs" : "Noirs") + " (bot) > " + chosen);
                game.play(chosen);
                continue;
            }

            String name = game.turn() == Color.WHITE ? "Blancs" : "Noirs";
            System.out.print(name + " > ");
            if (!in.hasNextLine()) return;
            String line = in.nextLine().trim().toLowerCase();
            if (line.equals("q")) return;
            if (line.equals("coups")) {
                moves.forEach(m -> System.out.println("  " + m));
                continue;
            }
            Move chosen = parse(line, moves);
            if (chosen == null) {
                System.out.println("Coup illégal ou ambigu. Tape 'coups' pour voir les coups possibles.");
                continue;
            }
            game.play(chosen);
        }
        System.out.println();
        System.out.print(game.board());
        System.out.println("Victoire des " + (game.winner() == Color.WHITE ? "Blancs" : "Noirs") + " !");
    }

    /**
     * Fait s'affronter {@link Bot} (minimax, stratégie gagnante) contre {@link RandomBot}
     * (coups aléatoires) sans aucune saisie humaine, en affichant après chaque coup le
     * temps, le nombre de noeuds explorés (côté minimax) et les octets alloués
     * (equivalent Java de {@code ThreadMXBean.getThreadAllocatedBytes}, comme dans
     * {@link Bench}) — utile pour comparer avant/après optimisation sur une partie entière.
     */
    static void runBotVsBot(Scanner in) {
        System.out.print("Le bot minimax (stratégie gagnante) joue les blancs ou les noirs ? (b/n, blancs par défaut) > ");
        String line = in.hasNextLine() ? in.nextLine().trim().toLowerCase() : "";
        Color strategicColor = line.startsWith("n") ? Color.BLACK : Color.WHITE;
        int depth = chooseDepth(in);

        Bot strategic = new Bot(depth);
        RandomBot random = new RandomBot();
        Game game = new Game();

        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();
        long totalNanos = 0;
        long totalAllocated = 0;
        long totalNodes = 0;
        int plies = 0;

        while (!game.isOver()) {
            System.out.println();
            System.out.print(game.board());
            Color turn = game.turn();
            boolean isStrategic = turn == strategicColor;
            String name = (turn == Color.WHITE ? "Blancs" : "Noirs") + (isStrategic ? " (bot minimax d=" + depth + ")" : " (bot random)");

            long allocBefore = bean.getThreadAllocatedBytes(threadId);
            long start = System.nanoTime();
            Move move = isStrategic ? strategic.chooseMove(game.board(), turn) : random.chooseMove(game.board(), turn);
            long elapsed = System.nanoTime() - start;
            long allocated = bean.getThreadAllocatedBytes(threadId) - allocBefore;
            if (move == null) break;

            totalNanos += elapsed;
            totalAllocated += allocated;
            plies++;
            if (isStrategic) {
                totalNodes += strategic.nodesExplored();
                System.out.printf("%s > %s   [%.1f ms, %d noeuds, %.1f Ko]%n",
                        name, move, elapsed / 1e6, strategic.nodesExplored(), allocated / 1024.0);
            } else {
                System.out.printf("%s > %s   [%.2f ms, %.1f Ko]%n", name, move, elapsed / 1e6, allocated / 1024.0);
            }
            game.play(move);
        }

        System.out.println();
        System.out.print(game.board());
        System.out.println("Victoire des " + (game.winner() == Color.WHITE ? "Blancs" : "Noirs") + " !");
        System.out.printf("Total : %d coups, %.0f ms cumulées, %.1f Mo allouées, %d noeuds minimax (%.0f noeuds/s)%n",
                plies, totalNanos / 1e6, totalAllocated / 1_048_576.0, totalNodes,
                totalNodes / (totalNanos / 1e9));
    }

    /** Demande si l'on joue contre le bot et, si oui, la couleur qu'il incarne. Renvoie {@code null} pour un jeu à deux joueurs humains. */
    static Color chooseOpponent(Scanner in) {
        System.out.print("Jouer contre le bot ? (o/n) > ");
        if (!in.hasNextLine()) return null;
        if (!in.nextLine().trim().toLowerCase().startsWith("o")) return null;

        System.out.print("Le bot joue les blancs ou les noirs ? (b/n, humain en blancs par défaut) > ");
        String line = in.hasNextLine() ? in.nextLine().trim().toLowerCase() : "";
        return line.startsWith("b") ? Color.WHITE : Color.BLACK;
    }

    /** Demande la profondeur de recherche du bot (niveau de difficulté). */
    static int chooseDepth(Scanner in) {
        System.out.print("Niveau du bot, profondeur de recherche (défaut 5) > ");
        if (!in.hasNextLine()) return 5;
        String line = in.nextLine().trim();
        try {
            int depth = Integer.parseInt(line);
            return depth > 0 ? depth : 5;
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    /** Cherche le coup correspondant : chemin complet, sinon départ/arrivée si unique. */
    static Move parse(String line, List<Move> moves) {
        try {
            String[] parts = line.split("[-x]");
            if (parts.length < 2) return null;
            List<Position> squares = java.util.Arrays.stream(parts).map(Position::parse).toList();
            for (Move m : moves) {
                if (m.squares().equals(squares)) return m;
            }
            if (squares.size() == 2) {
                List<Move> matches = moves.stream()
                        .filter(m -> m.from().equals(squares.get(0)) && m.to().equals(squares.get(1))).toList();
                if (matches.size() == 1) return matches.get(0);
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
