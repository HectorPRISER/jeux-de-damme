package dames;

import org.junit.jupiter.api.Test;

/**
 * Charge CPU pour le profilage (voir README, section Profilage).
 * Pas de suffixe "Test" : ignoré par `mvn test`, à lancer explicitement
 * avec `mvn test -Dtest=BotBench`.
 */
class BotBench {
    @Test
    void run() {
        Bot bot = new Bot(7);
        Board board = Board.initial();
        Color color = Color.WHITE;
        for (int ply = 0; ply < 3; ply++) {
            Move move = bot.chooseMove(board, color);
            if (move == null) break;
            board.apply(move);
            color = color.opposite();
        }
    }
}
