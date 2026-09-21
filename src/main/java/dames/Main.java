package dames;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Game game = new Game();
        Scanner in = new Scanner(System.in);
        System.out.println("Jeu de dames internationales (10x10). Blancs (w/W) en bas, noirs (b/B) en haut.");
        System.out.println("Saisie : c3-d4 (déplacement) ou c3-e5-g7 (rafle, ou juste départ-arrivée). 'coups' liste les coups, 'q' quitte.");

        while (!game.isOver()) {
            System.out.println();
            System.out.print(game.board());
            List<Move> moves = game.legalMoves();
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
