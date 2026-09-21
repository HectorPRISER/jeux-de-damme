package dames;

public record Position(int row, int col) {
    public Position plus(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }

    /** Notation algébrique : a1 en bas à gauche, j10 en haut à droite. */
    @Override
    public String toString() {
        return "" + (char) ('a' + col) + (Board.SIZE - row);
    }

    public static Position parse(String s) {
        s = s.trim().toLowerCase();
        if (s.length() < 2 || s.length() > 3) {
            throw new IllegalArgumentException("Case invalide : " + s);
        }
        int col = s.charAt(0) - 'a';
        int rank;
        try {
            rank = Integer.parseInt(s.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Case invalide : " + s);
        }
        Position p = new Position(Board.SIZE - rank, col);
        if (!Board.inBounds(p)) {
            throw new IllegalArgumentException("Case hors du plateau : " + s);
        }
        return p;
    }
}
