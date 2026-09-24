package dames;

import java.util.ArrayList;
import java.util.List;

/** Règles internationales : prise obligatoire, prise majoritaire, prise arrière, dames volantes. */
public final class MoveGenerator {
    private static final int[][] DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private MoveGenerator() {}

    public static List<Move> legalMoves(Board board, Color color) {
        List<Move> captures = new ArrayList<>();
        List<Move> simple = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece piece = board.get(r, c);
                if (piece == null || piece.color() != color) continue;
                Position pos = new Position(r, c);
                List<Position> path = new ArrayList<>(List.of(pos));
                collectCaptures(board, pos, pos, piece, path, new ArrayList<>(), captures);
                simple.addAll(simpleMoves(board, pos, piece));
            }
        }
        if (captures.isEmpty()) return simple;
        int max = captures.stream().mapToInt(m -> m.captured().size()).max().getAsInt();
        return captures.stream().filter(m -> m.captured().size() == max).toList();
    }

    private static List<Move> simpleMoves(Board board, Position from, Piece piece) {
        List<Move> moves = new ArrayList<>();
        for (int[] d : DIRS) {
            if (!piece.king() && d[0] != piece.color().forward()) continue;
            Position p = from.plus(d[0], d[1]);
            while (Board.inBounds(p) && board.isEmpty(p)) {
                moves.add(new Move(List.of(from, p), List.of()));
                if (!piece.king()) break;
                p = p.plus(d[0], d[1]);
            }
        }
        return moves;
    }

    /** Explore les rafles ; pièces prises restent sur le plateau (bloquent, non reprenables), départ vu comme vide. */
    private static void collectCaptures(Board board, Position origin, Position cur, Piece piece,
                                        List<Position> path, List<Position> captured, List<Move> out) {
        boolean extended = false;
        for (int[] d : DIRS) {
            Position p = cur.plus(d[0], d[1]);
            if (piece.king()) {
                while (Board.inBounds(p) && isFree(board, p, origin)) {
                    p = p.plus(d[0], d[1]);
                }
            }
            if (!Board.inBounds(p)) continue;
            Piece target = board.get(p);
            if (target == null || target.color() == piece.color() || captured.contains(p)) continue;

            Position land = p.plus(d[0], d[1]);
            while (Board.inBounds(land) && isFree(board, land, origin)) {
                extended = true;
                captured.add(p);
                path.add(land);
                collectCaptures(board, origin, land, piece, path, captured, out);
                path.remove(path.size() - 1);
                captured.remove(captured.size() - 1);
                if (!piece.king()) break;
                land = land.plus(d[0], d[1]);
            }
        }
        if (!extended && !captured.isEmpty()) {
            out.add(new Move(List.copyOf(path), List.copyOf(captured)));
        }
    }

    private static boolean isFree(Board board, Position p, Position origin) {
        return board.isEmpty(p) || p.equals(origin);
    }
}
