// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import java.util.*;

public class Heuristic {
    // central positions
    private static final Set<Integer> CENTER_POSITIONS = new HashSet<>(Arrays.asList(
            21, 22, 25, 26,
            29, 30, 33, 34
    ));

    // middle plane corner positions
    private static final Set<Integer> PCORNER_POSITIONS = new HashSet<>(Arrays.asList(
            16, 19, 28, 31,
            32, 35, 44, 47
    ));

    // board corner positions
    private static final Set<Integer> BCORNER_POSITIONS = new HashSet<>(Arrays.asList(
            0, 3, 12, 15,
            48, 51, 60, 63
    ));

    private static long computePositionMask(Set<Integer> positions) {
        long mask = 0L;
        for (int pos : positions) {
            mask = Bit.set(mask, pos);
        }
        return mask;
    }

    private static final long CENTER_POSITIONS_MASK = computePositionMask(CENTER_POSITIONS);
    private static final long PCORNER_POSITIONS_MASK = computePositionMask(PCORNER_POSITIONS);
    private static final long BCORNER_POSITIONS_MASK = computePositionMask(BCORNER_POSITIONS);


    /**
     * Evaluates the board from the perspective of the specified player.
     *
     * @param board  The current board state.
     * @param player The player for whom the evaluation is being performed.
     * @return An integer score representing the desirability of the board state.
     */
    public static long evaluate(Board board, Player player, Weights w) {
        int score = 0;
        Player opponent = player.other();

        long playerPositions = (player == Player.X) ? board.xPositions : board.oPositions;
        long opponentPositions = (player == Player.X) ? board.oPositions : board.xPositions;
        long occupiedPositions = playerPositions | opponentPositions;

        for (Line line : Line.lines) {
            long linePositions = line.positions();

            long playerLinePositions = linePositions & playerPositions;
            long opponentLinePositions = linePositions & opponentPositions;
            long emptyLinePositions = linePositions & ~occupiedPositions;

            int playerCount = Bit.countOnes(playerLinePositions);
            int opponentCount = Bit.countOnes(opponentLinePositions);
            int emptyCount = Bit.countOnes(emptyLinePositions);

            if (playerCount > 0 && opponentCount == 0) {
                // open for the player
                int centerCount = Bit.countOnes(playerLinePositions & CENTER_POSITIONS_MASK);
                score += getScore(playerCount, centerCount, w);
            } else if (opponentCount > 0 && playerCount == 0) {
                // open for the opponent
                int centerCount = Bit.countOnes(opponentLinePositions & CENTER_POSITIONS_MASK);
                score -= getScore(opponentCount, centerCount, w) * w.OPPONENT_SCORE_MULTIPLIER;
            }

            if (playerCount == 2 && emptyCount == 2) {
                List<Integer> emptyPositionsList = Bit.onesList(emptyLinePositions);

                for (int candidatePos : emptyPositionsList) {
                    long hypotheticalPlayerPositions = Bit.set(playerPositions, candidatePos);
                    long hypotheticalOccupied = occupiedPositions | Bit.positionMask(candidatePos);

                    // Evaluate future forks if we place here
                    int forksAfterPlacement = evaluatePotentialForks(hypotheticalPlayerPositions, hypotheticalOccupied);
                    Board hypotheticalBoard = new Board();
                    if (player == Player.X) {
                        hypotheticalBoard.xPositions = hypotheticalPlayerPositions;
                        hypotheticalBoard.oPositions = opponentPositions;
                    } else {
                        hypotheticalBoard.xPositions = opponentPositions;
                        hypotheticalBoard.oPositions = hypotheticalPlayerPositions;
                    }

                    int moveCount = board.totalMoves();
                    if (forksAfterPlacement > 0) {
                        if (moveCount < 10) {
                            score += w.SCORE_THREE;
                        } else {
                            score += w.SCORE_THREE * 4;
                        }
                    } else {
                        score += w.SCORE_TWO * 2;
                    }
                }
            }

            // three in a row and an empty spot (immediate threat)
            if (opponentCount == 3 && emptyCount == 1) {
                score -= w.IMMEDIATE_THREAT_PENALTY;
            }

            // three in a row and an empty spot (immediate win possibility)
            if (playerCount == 3 && emptyCount == 1) {
                score += w.IMMEDIATE_WIN_BONUS;
            }
        }

        // central control
        int playerCenterControl = Bit.countOnes(playerPositions & CENTER_POSITIONS_MASK);
        int opponentCenterControl = Bit.countOnes(opponentPositions & CENTER_POSITIONS_MASK);
        score += playerCenterControl * w.CENTER_CONTROL_MULTIPLIER;
        score -= opponentCenterControl * w.OPPONENT_CENTER_CONTROL_MULTIPLIER;

        // corner control
        int playerPCorners = Bit.countOnes(playerPositions & PCORNER_POSITIONS_MASK);
        int opponentPCorners = Bit.countOnes(opponentPositions & PCORNER_POSITIONS_MASK);
        score += playerPCorners * w.PCORNER_CONTROL_MULTIPLIER;
        score -= opponentPCorners * w.OPPONENT_PCORNER_CONTROL_MULTIPLIER;

        int playerBCorners = Bit.countOnes(playerPositions & BCORNER_POSITIONS_MASK);
        int opponentBCorners = Bit.countOnes(opponentPositions & BCORNER_POSITIONS_MASK);
        score += playerBCorners * w.BCORNER_CONTROL_MULTIPLIER;
        score -= opponentBCorners * w.OPPONENT_BCORNER_CONTROL_MULTIPLIER;

        // potential forks
        int playerForks = evaluatePotentialForks(playerPositions, occupiedPositions);
        int opponentForks = evaluatePotentialForks(opponentPositions, occupiedPositions);
        score += playerForks * w.PLAYER_FORKS_MULTIPLIER;
        score -= opponentForks * w.OPPONENT_FORKS_MULTIPLIER;

        // opponent's potential forks in next move
        int opponentPotentialForks = evaluateOpponentPotentialForks(board, opponent, occupiedPositions);
        score -= opponentPotentialForks * w.OPPONENT_POTENTIAL_FORKS_PENALTY;

        return score;
    }

    private static int getScore(int count, int centerCount, Weights w) {
        int baseScore = switch (count) {
            case 1 -> w.SCORE_ONE;
            case 2 -> w.SCORE_TWO;
            case 3 -> w.SCORE_THREE;
            case 4 -> w.SCORE_FOUR;
            default -> 0;
        };

        baseScore += centerCount * w.CENTER_MULTIPLIER;

        return baseScore;
    }

    /**
     * Evaluates potential forks for the player.
     *
     * @param playerPositions   Bitmask of player's positions.
     * @param occupiedPositions Bitmask of occupied positions.
     * @return The number of potential forks.
     */
    private static int evaluatePotentialForks(long playerPositions, long occupiedPositions) {
        int forkCount = 0;

        List<Long> potentialLines = new ArrayList<>();

        for (Line line : Line.lines) {
            long linePositions = line.positions();

            long playerLinePositions = linePositions & playerPositions;
            long emptyLinePositions = linePositions & ~occupiedPositions;

            int playerCount = Bit.countOnes(playerLinePositions);
            int emptyCount = Bit.countOnes(emptyLinePositions);

            if (playerCount == 2 && emptyCount == 2) {
                potentialLines.add(linePositions);
            }
        }

        // overlapping positions in different lines
        for (int i = 0; i < potentialLines.size(); i++) {
            for (int j = i + 1; j < potentialLines.size(); j++) {
                long intersection = potentialLines.get(i) & potentialLines.get(j) & ~occupiedPositions;
                if (Bit.countOnes(intersection) > 0) {
                    forkCount++;
                }
            }
        }

        return forkCount;
    }

    /**
     * Evaluates the opponent's potential to create forks in their next move.
     *
     * @param board             The current board state.
     * @param opponent          The opponent player.
     * @param occupiedPositions Bitmask of occupied positions.
     * @return The number of potential forks the opponent can create in their next move.
     */
    private static int evaluateOpponentPotentialForks(Board board, Player opponent, long occupiedPositions) {
        int potentialForks = 0;

        // simulate the opponent's move
        List<Integer> emptyPositions = Bit.onesList(~occupiedPositions);

        for (int position : emptyPositions) {
            long newOpponentPositions = (opponent == Player.X) ? Bit.set(board.xPositions, position) : Bit.set(board.oPositions, position);
            long newOccupiedPositions = occupiedPositions | Bit.positionMask(position);

            // num forks opponent can create from this move
            int forks = evaluatePotentialForks(newOpponentPositions, newOccupiedPositions);
            if (forks > 0) {
                potentialForks += forks;
            }
        }

        return potentialForks;
    }
}