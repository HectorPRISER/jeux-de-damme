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

    public void set(int row, int col, Piece piece) {
        cells[index(row, col)] = piece;
    }

    public void set(Position p, Piece piece) {
        set(p.row(), p.col(), piece);
    }

    public boolean isEmpty(Position p) {
        return get(p) == null;
    }

    /** Copie indépendante du plateau — pour isoler un thread (racine du minimax), pas pour la boucle chaude (voir apply/undo). */
    public Board copy() {
        Board b = new Board();
        System.arraycopy(cells, 0, b.cells, 0, cells.length);
        return b;
    }

    /** Ce qu'il faut pour annuler un {@link #apply} : la pièce déplacée (avant promotion) et les pièces prises. */
    public record Undo(Piece movedPiece, Piece[] capturedPieces) {}

    /** Joue le coup en mutant ce plateau (pas de copie) ; retourne l'état à repasser à {@link #undo}. */
    public Undo apply(Move move) {
        Position from = move.from();
        Piece movedPiece = get(from);
        set(from, null);

        List<Position> capturedSquares = move.captured();
        Piece[] capturedPieces = new Piece[capturedSquares.size()];
        for (int i = 0; i < capturedPieces.length; i++) {
            Position p = capturedSquares.get(i);
            capturedPieces[i] = get(p);
            set(p, null);
        }

        Position to = move.to();
        Piece piece = movedPiece;
        if (!piece.king() && to.row() == piece.color().promotionRow()) {
            piece = piece.promoted();
        }
        set(to, piece);

        return new Undo(movedPiece, capturedPieces);
    }

    /** Défait le dernier {@link #apply} de ce {@code move} sur ce plateau. */
    public void undo(Move move, Undo undo) {
        set(move.to(), null);
        set(move.from(), undo.movedPiece());

        List<Position> capturedSquares = move.captured();
        for (int i = 0; i < undo.capturedPieces().length; i++) {
            set(capturedSquares.get(i), undo.capturedPieces()[i]);
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
