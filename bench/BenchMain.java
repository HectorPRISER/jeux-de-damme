import dames.Board;
import dames.Bot;
import dames.Color;
import dames.Move;

/**
 * Charge de travail du benchmark : le bot joue {@code nbCoups} demi-coups depuis la position initiale.
 *
 * Les coups joués sont affichés : c'est une « somme de contrôle » qui prouve qu'une version optimisée
 * joue exactement les mêmes coups que la version de départ (une optimisation ne doit pas changer le résultat).
 *
 * Usage : java BenchMain [profondeur=6] [nbCoups=3]
 */
public class BenchMain {
    public static void main(String[] args) {
        int depth = args.length > 0 ? Integer.parseInt(args[0]) : 6;
        int plies = args.length > 1 ? Integer.parseInt(args[1]) : 3;

        Bot bot = new Bot(depth);
        Board board = Board.initial();
        Color color = Color.WHITE;
        StringBuilder played = new StringBuilder();
        for (int i = 0; i < plies; i++) {
            Move move = bot.chooseMove(board, color);
            if (move == null) break;
            played.append(move).append(' ');
            board.apply(move);
            color = color.opposite();
        }
        System.out.println(played.toString().trim());
    }
}
