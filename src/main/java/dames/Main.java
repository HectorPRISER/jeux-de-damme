package dames;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Jeu de dames internationales (10x10). Blancs (w/W) en bas, noirs (b/B) en haut.");
        System.out.println("Saisie : c3-d4 (déplacement) ou c3-e5-g7 (rafle, ou juste départ-arrivée). 'coups' liste les coups, 'q' quitte.");

        Color botColor = chooseOpponent(in);
        Bot bot = botColor == null ? null : new Bot(chooseDepth(in));
        Game game = new Game();

        while (!game.isOver()) {
            System.out.println();
            System.out.print(game.board());
            List<Move> moves = game.legalMoves();

            if (game.turn() == botColor) {
                Move chosen = bot.chooseMove(game.board(), botColor);
                System.out.println(botColor.label() + " (bot) > " + chosen);
                game.play(chosen);
                continue;
            }

            String name = game.turn().label();
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
        System.out.println("Victoire des " + game.winner().label() + " !");
    }

    /** {@code null} = deux humains. */
    static Color chooseOpponent(Scanner in) {
        System.out.print("Jouer contre le bot ? (o/n) > ");
        if (!in.hasNextLine()) return null;
        if (!in.nextLine().trim().toLowerCase().startsWith("o")) return null;

        System.out.print("Le bot joue les blancs ou les noirs ? (b/n, humain en blancs par défaut) > ");
        String line = in.hasNextLine() ? in.nextLine().trim().toLowerCase() : "";
        return line.startsWith("b") ? Color.WHITE : Color.BLACK;
    }

    /** Profondeur minimax. */
    static int chooseDepth(Scanner in) {
        System.out.print("Niveau du bot, profondeur de recherche (défaut 5) > ");
        return in.hasNextLine() ? parseDepth(in.nextLine().trim()) : 5;
    }

    static int parseDepth(String value) {
        try {
            int depth = Integer.parseInt(value);
            return depth > 0 ? depth : 5;
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    /** Chemin complet, sinon départ/arrivée si unique. */
    static Move parse(String line, List<Move> moves) {
        try {
            String[] parts = line.split("[-x]");
            if (parts.length < 2) return null;
            List<Position> squares = Arrays.stream(parts).map(Position::parse).toList();
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
