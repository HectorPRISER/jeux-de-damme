package dames;

public class Board {
    public static final int SIZE = 10;

    private final Piece[][] cells = new Piece[SIZE][SIZE];

    public static boolean inBounds(Position p) {
        return p.row() >= 0 && p.row() < SIZE && p.col() >= 0 && p.col() < SIZE;
    }

    public static boolean isDark(int row, int col) {
        return (row + col) % 2 == 1;
    }

    public static Board initial() {
        Board b = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!isDark(r, c)) continue;
                if (r < 4) b.cells[r][c] = new Piece(Color.BLACK, false);
                else if (r >= SIZE - 4) b.cells[r][c] = new Piece(Color.WHITE, false);
            }
        }
        return b;
    }

    public static Board empty() {
        return new Board();
    }

    public Piece get(Position p) {
        return cells[p.row()][p.col()];
    }

    public void set(Position p, Piece piece) {
        cells[p.row()][p.col()] = piece;
    }

    public boolean isEmpty(Position p) {
        return get(p) == null;
    }

    public Board copy() {
        Board b = new Board();
        for (int r = 0; r < SIZE; r++) {
            b.cells[r] = cells[r].clone();
        }
        return b;
    }

    /** Joue le coup : retire les pièces prises, déplace, promeut si arrivée sur la dernière ligne. */
    public void apply(Move move) {
        Piece piece = get(move.from());
        set(move.from(), null);
        for (Position p : move.captured()) {
            set(p, null);
        }
        Position to = move.to();
        if (!piece.king() && to.row() == piece.color().promotionRow()) {
            piece = piece.promoted();
        }
        set(to, piece);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            sb.append(String.format("%2d ", SIZE - r));
            for (int c = 0; c < SIZE; c++) {
                Piece p = cells[r][c];
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
