package dames;

public enum Color {
    WHITE, BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** Direction de déplacement des pions (en lignes) : les blancs montent. */
    public int forward() {
        return this == WHITE ? -1 : 1;
    }

    /** Ligne de promotion. */
    public int promotionRow() {
        return this == WHITE ? 0 : Board.SIZE - 1;
    }
}
