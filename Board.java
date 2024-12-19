// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import java.util.ArrayList;
import java.util.List;

public class Board {

    public static final int N = Coordinate.N;
    public long xPositions = 0L;
    public long oPositions = 0L;

    public Board() {
    }

    public Player get(int position) {
        if (Bit.isSet(xPositions, position)) {
            return Player.X;
        } else if (Bit.isSet(oPositions, position)) {
            return Player.O;
        } else {
            return null;
        }
    }

    public Player get(int x, int y, int z) {
        int position = Coordinate.position(x, y, z);
        return get(position);
    }

    public void set(int position, Player player) {
        if (player == Player.X) {
            xPositions = Bit.set(xPositions, position);
            oPositions = Bit.clear(oPositions, position);
        } else if (player == Player.O) {
            oPositions = Bit.set(oPositions, position);
            xPositions = Bit.clear(xPositions, position);
        } else {
            xPositions = Bit.clear(xPositions, position);
            oPositions = Bit.clear(oPositions, position);
        }
    }

    public boolean isFull() {
        return Bit.countOnes(xPositions | oPositions) == 64;
    }

    public boolean isEmpty() {
        return Bit.countOnes(xPositions | oPositions) == 0;
    }

    public int totalMoves() {
        return Bit.countOnes(xPositions | oPositions);
    }

    public List<Integer> getAvailableMoves() {
        List<Integer> moves = new ArrayList<>();
        long occupied = xPositions | oPositions;
        for (int position = 0; position < 64; position++) {
            if (!Bit.isSet(occupied, position)) {
                moves.add(position);
            }
        }
        return moves;
    }

    public boolean isValidMove(int position) {
        long occupied = xPositions | oPositions;
        return !Bit.isSet(occupied, position);
    }

    public void makeMove(int position, Player player) {
        set(position, player);
    }

    public void undoMove(int position) {
        set(position, null);
    }

    // Construct a Board from a string representation.
    // Should be an inverse function of toString().

    public static Board valueOf(String s) {
        Board board = new Board();
        int position = 0;

        for (int i= 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'x':
                case 'X':
                    board.set(position++, Player.X);
                    break;

                case 'o':
                case 'O':
                    board.set(position++, Player.O);
                    break;

                case '.':
                    position++;
                    break;

                case ' ':
                case '|':
                    break;

                default:
                    throw new IllegalArgumentException("Invalid player: " + c);
            }
        }
        return board;
    }

    // Image & printing functions.

    /*@Override
    public String toString() {
        String result = "";
        String separator = "";

        for (int position = 0; position < 64; position++) {
            Player player = this.get(position);
            result += separator;
            result += (player == null) ? "." : player.toString();
            if ((position + 1) % 16 == 0) {
                separator = " | ";
            } else if ((position + 1) % 4 == 0) {
                separator = " ";
            } else {
                separator = "";
            }
        }
        return result;
    }*/
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int position = 0; position < 64; position++) {
            Player player = this.get(position);
            result.append((player == null) ? "." : player.toString());
        }
        return result.toString();
    }

    public void print() {
        for (int y = N - 1; y >= 0; y--) {
            for (int z = 0; z < N; z++) {
                for (int x = 0; x < N; x++) {
                    Player player = this.get(x, y, z);
                    System.out.print((player == null) ? "." : player.toString());
                }
                System.out.print("    ");
            }
            System.out.println();
        }
    }

    public boolean hasWon(Player nextPlayer) {
        long positions = (nextPlayer == Player.X) ? xPositions : oPositions;
        for (Line line : Line.lines) {
            if ((positions & line.positions()) == line.positions()) {
                return true;
            }
        }
        return false;
    }

    public int getRandomEmptyPosition() {
        List<Integer> availableMoves = getAvailableMoves();
        int randomIndex = (int) (Math.random() * availableMoves.size());
        return availableMoves.get(randomIndex);
    }

    public Line getWinningLine(Player player) {
        long positions = (player == Player.X) ? xPositions : oPositions;
        for (Line line : Line.lines) {
            if ((positions & line.positions()) == line.positions()) {
                return line;
            }
        }
        return null;
    }
}