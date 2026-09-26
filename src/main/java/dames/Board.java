package dames;

import java.util.List;

public class Board {
    public static final int SIZE = 10;

    private final Piece[] cells = new Piece[SIZE * SIZE];

    public static boolean inBounds(Position p) {
        return p.row() >= 0 && p.row() < SIZE && p.col() >= 0 && p.col() < SIZE;
    }

    public static boolean isDark(int row, int col) {
        return (row + col) % 2 == 1;
    }

    private static int index(int row, int col) {
        return row * SIZE + col;
    }

    public static Board initial() {
        Board b = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!isDark(r, c)) continue;
                if (r < 4) b.cells[index(r, c)] = new Piece(Color.BLACK, false);
                else if (r >= SIZE - 4) b.cells[index(r, c)] = new Piece(Color.WHITE, false);
            }
        }
        return b;
    }

    public static Board empty() {
        return new Board();
    }

    public Piece get(int row, int col) {
        return cells[index(row, col)];
    }

    public Piece get(Position p) {
        return get(p.row(), p.col());
    }

    public void set(Position p, Piece piece) {
        cells[index(p.row(), p.col())] = piece;
    }

    public boolean isEmpty(Position p) {
        return get(p) == null;
    }

    /** De quoi annuler un coup : la pièce déplacée (avant promotion éventuelle) et les pièces prises. */
    public record Undo(Piece moved, Piece[] captured) {}

    /** Joue le coup sur ce plateau, sans le copier : retire les pièces prises, déplace, promeut si besoin. */
    public Undo apply(Move move) {
        Piece moved = get(move.from());
        Piece[] captured = new Piece[move.captured().size()];
        set(move.from(), null);
        for (int i = 0; i < captured.length; i++) {
            captured[i] = get(move.captured().get(i));
            set(move.captured().get(i), null);
        }
        boolean promotes = !moved.king() && move.to().row() == moved.color().promotionRow();
        set(move.to(), promotes ? moved.promoted() : moved);
        return new Undo(moved, captured);
    }

    /** Annule un {@link #apply} : remet la pièce déplacée et les pièces prises. */
    public void undo(Move move, Undo undo) {
        set(move.to(), null);
        set(move.from(), undo.moved());
        for (int i = 0; i < undo.captured().length; i++) {
            set(move.captured().get(i), undo.captured()[i]);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            sb.append(String.format("%2d ", SIZE - r));
            for (int c = 0; c < SIZE; c++) {
                Piece p = get(r, c);
                char ch;
                if (p == null) ch = isDark(r, c) ? '.' : ' ';
                else if (p.color() == Color.WHITE) ch = p.king() ? 'W' : 'w';
                else ch = p.king() ? 'B' : 'b';
                sb.append(ch).append(' ');
            }
            sb.append('\n');
        }
        sb.append("   ");
        for (int c = 0; c < SIZE; c++) sb.append((char) ('a' + c)).append(' ');
        return sb.append('\n').toString();
    }
}
