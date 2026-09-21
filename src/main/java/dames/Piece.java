package dames;

public record Piece(Color color, boolean king) {
    public Piece promoted() {
        return new Piece(color, true);
    }
}
