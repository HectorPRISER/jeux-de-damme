package dames;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Un coup : la suite des cases visitées (départ inclus) et les pièces prises.
 */
public record Move(List<Position> squares, List<Position> captured) {
    public Position from() {
        return squares.get(0);
    }

    public Position to() {
        return squares.get(squares.size() - 1);
    }

    public boolean isCapture() {
        return !captured.isEmpty();
    }

    @Override
    public String toString() {
        return squares.stream().map(Position::toString)
                .collect(Collectors.joining(isCapture() ? "x" : "-"));
    }
}
