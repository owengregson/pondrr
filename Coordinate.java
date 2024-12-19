// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

public class Coordinate {

    // A class to facilitate conversion between positions and XYZ coordinates.

    public static final int N = 4;
    public static final int NSquared = N * N;
    public static final int NCubed = N * N * N;

    public static int getX(int position) {
        return position % N;
    }

    public static int getY(int position) {
        return (position / N) % N;
    }

    public static int getZ(int position) {
        return position / (N * N);
    }

    public static int position(int x, int y, int z) {
        return x + N * y + N * N * z;
    }
}
