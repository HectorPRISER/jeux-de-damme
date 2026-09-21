package dames;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class MoveGeneratorTest {
    private static Position p(String s) {
        return Position.parse(s);
    }

    private static Piece w() { return new Piece(Color.WHITE, false); }
    private static Piece b() { return new Piece(Color.BLACK, false); }

    @Test
    void initialPositionHasNineMovesForWhite() {
        assertEquals(9, MoveGenerator.legalMoves(Board.initial(), Color.WHITE).size());
    }

    @Test
    void captureIsMandatory() {
        Board board = Board.empty();
        board.set(p("d4"), w());
        board.set(p("e5"), b());
        board.set(p("h2"), w());
        List<Move> moves = MoveGenerator.legalMoves(board, Color.WHITE);
        assertEquals(1, moves.size());
        assertEquals("d4xf6", moves.get(0).toString());
    }

    @Test
    void menCaptureBackwards() {
        Board board = Board.empty();
        board.set(p("d4"), w());
        board.set(p("c3"), b());
        List<Move> moves = MoveGenerator.legalMoves(board, Color.WHITE);
        assertEquals(List.of("d4xb2"), moves.stream().map(Move::toString).toList());
    }

    @Test
    void majorityCaptureRuleApplies() {
        Board board = Board.empty();
        board.set(p("a1"), w());
        board.set(p("b2"), b());       // prise simple possible
        board.set(p("h2"), w());
        board.set(p("g3"), b());
        board.set(p("e5"), b());       // h2 peut prendre g3 puis e5
        List<Move> moves = MoveGenerator.legalMoves(board, Color.WHITE);
        assertEquals(1, moves.size());
        assertEquals(2, moves.get(0).captured().size());
    }

    @Test
    void kingFliesAndLandsAnywhereBehindCapturedPiece() {
        Board board = Board.empty();
        board.set(p("a1"), new Piece(Color.WHITE, true));
        board.set(p("d4"), b());
        List<Move> moves = MoveGenerator.legalMoves(board, Color.WHITE);
        assertEquals(List.of("a1xe5", "a1xf6", "a1xg7", "a1xh8", "a1xi9", "a1xj10"),
                moves.stream().map(Move::toString).sorted().toList());
    }

    @Test
    void pieceCannotBeCapturedTwice() {
        Board board = Board.empty();
        board.set(p("a1"), w());
        board.set(p("b2"), b());
        board.set(p("b4"), b());
        board.set(p("d4"), b());
        board.set(p("d2"), b());
        // boucle a1xc3xa5? : chaque pièce prise au plus une fois
        for (Move m : MoveGenerator.legalMoves(board, Color.WHITE)) {
            assertEquals(m.captured().size(), m.captured().stream().distinct().count());
        }
    }

    @Test
    void manPromotesOnlyWhenEndingOnLastRow() {
        Board board = Board.empty();
        board.set(p("b9"), w());
        Game game = new Game(board, Color.WHITE);
        game.play(game.legalMoves().stream().filter(m -> m.to().equals(p("a10"))).findFirst().orElseThrow());
        assertTrue(game.board().get(p("a10")).king());
    }
}
