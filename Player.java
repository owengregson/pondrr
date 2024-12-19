// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #1
// Nov 12, 2024

public enum Player {
    X, O;

    public Player other() {
        return this == X ? O : X;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
